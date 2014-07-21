package jobs.steps

import com.recomdata.transmart.util.RUtil
import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Log4j
import jobs.UserParameters
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException

@Log4j
class RCommandsStep implements Step {

    final String statusName = 'Running analysis'

    File temporaryDirectory
    File scriptsDirectory
    UserParameters params
    List<String> rStatements
    String studyName /* see comment on AbstractAnalysisJob::studyName */

    /**
     * This map allows us to pass information that is only available later on
     */
    Map<String, String> extraParams

    @Override
    void execute() {
        runRCommandList rStatements
    }

    final private void runRCommandList(List<String> stepList) {
        //Establish a connection to R Server.
        RConnection rConnection = new RConnection()

        try {
            //Run the R command to set the working directory to our temp directory.
            String wd = RUtil.escapeRStringContent(temporaryDirectory.absolutePath)
            log.info "About to trigger R command: setwd('$wd')"
            rConnection.eval "setwd('${wd}')"

            /**
             * Please make sure that any and all variables you add to the map here are placed _after_ the putAll
             * as otherwise you create a potential security vulnerability
             */
            Map vars = [:]

            params.each { k,v ->
                vars[k] = v
            }

            //lazy extra params are added after user params, to make sure there are no security breaches
            if (extraParams) {
                vars.putAll(extraParams)
            }

            vars.pluginDirectory = scriptsDirectory.absolutePath
            vars.temporaryDirectory = new File(temporaryDirectory, "subset1_$studyName").absolutePath

            //For each R step there is a list of commands.
            stepList.each { String currentCommand ->
                runRCommand rConnection, currentCommand, vars
            }
        } finally {
            rConnection.close()
        }
    }

    private void runRCommand(RConnection connection, String command, Map vars) {
        String finalCommand = processTemplates command, vars
        log.info "About to trigger R command: $finalCommand"

        // TODO Set back silent mode REXP rObject = rConnection.parseAndEval("try($finalCommand, silent=TRUE)")
        REXP rObject = connection.parseAndEval("try($finalCommand, silent=FALSE)")

        if (rObject.inherits("try-error")) {
            log.error "R command failure for:$finalCommand"
            handleRError(rObject, connection)
        }
    }

    static private void handleRError(REXP rObject, RConnection rConnection) throws RserveException {
        throw new RserveException(rConnection,
                'There was an error running the R script for your job. ' +
                        "Details: ${rObject.asString()}")
    }

    static private String processTemplates(String template, Map vars) {
        Map escapedVars = [:].withDefault { k ->
            if (vars.containsKey(k)) {
                RUtil.escapeRStringContent vars[k].toString()
            }
        }
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        engine.createTemplate(template).make(escapedVars)
    }
}
