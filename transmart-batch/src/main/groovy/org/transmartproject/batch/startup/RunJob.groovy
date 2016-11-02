package org.transmartproject.batch.startup

import groovy.util.logging.Slf4j
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.converter.JobParametersConverter
import org.springframework.batch.core.launch.support.CommandLineJobRunner

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Entry point for the application.
 */
@SuppressWarnings(['SystemExit', 'SystemErrPrint'])
@Slf4j
final class RunJob {

    public static final String DEFAULT_BATCHDB_PROPERTIES_LOCATION = 'file:./batchdb.properties'
    public static final int SUCCESS_EXIT_CODE = 0
    public static final int FAILURE_EXIT_CODE = 1

    final OptionAccessor opts

    JobParameters finalJobParameters

    String jobName

    CommandLineJobRunner commandLineJobRunner = new CommandLineJobRunner()

    RunJob(OptionAccessor opts) {
        this.opts = opts
        ensurePropertySource()
    }

    private final static String USAGE = '''transmart-batch-capsule.jar -p <params file>
    [ -d <param=value> | [ -d <param2=value2> | ... ]]
    [ -c <file> ]
    (
        ((-r | -s | -a ) -j <job id>)) |
        [-n])'''

    private static CliBuilder createCliBuilder() {
        def cli = new CliBuilder(usage: USAGE)
        cli.writer = new PrintWriter(System.err)
        cli.with {
            d args: 2, valueSeparator: '=', argName: 'param=value', 'override/supplement params file parameter'
            p args: 1, argName: 'file location', 'specify params file'
            f args: 1, argName: 'folder location', 'specify folder with params files'
            c longOpt: 'config', args: 1, 'location of database configuration properties file ' +
                    "(default: $DEFAULT_BATCHDB_PROPERTIES_LOCATION)"
            j longOpt: 'jobIdentifier', args: 1, "the id or name of a job instance"
            r longOpt: 'restart', 'restart the last failed execution'
            s longOpt: 'stop', 'stop a running execution'
            a longOpt: 'abandon', 'abandon a stopped execution'
            n longOpt: 'next', 'start the next in a sequence according to the JobParametersIncrementer'
            it
        }
    }

    static void main(String... args) {
        def runJob = createInstance(args)
        def exitCode = runJob.run()
        System.exit exitCode
    }

    static RunJob createInstance(String... args) {
        def cliBuilder = createCliBuilder()
        OptionAccessor opts = cliBuilder.parse(args)
        RunJob runJobInstance = new RunJob(opts)
        runJobInstance
    }

    private void ensurePropertySource() {
        System.setProperty 'propertySource', batchPropertiesPath
    }

    int run() {
        def jobsInitDetails = jobsStartupDetails
        if (jobsInitDetails) {
            log.info("Following params files found and parsed: ${jobsInitDetails}")
        } else {
            log.info("No params files found.")
            return SUCCESS_EXIT_CODE
        }
        boolean hasFailure = jobsInitDetails.any { JobStartupDetails jobDetails ->
            log.info("Start processing ${jobDetails}")
            startJob(jobDetails) > SUCCESS_EXIT_CODE
        }

        hasFailure ? FAILURE_EXIT_CODE : SUCCESS_EXIT_CODE
    }

    private int startJob(JobStartupDetails jobInitializationData) {
        JobParametersConverter jobParametersConverter = getJobParametersConverterForJobDetails(jobInitializationData)
        finalJobParameters = jobParametersConverter.getJobParameters(null)
        commandLineJobRunner.jobParametersConverter = jobParametersConverter

        String jobIdentifier = calculateJobIdentifier(jobInitializationData.jobPath)
        jobName = jobInitializationData.jobPath.JOB_NAME

        commandLineJobRunner.start(jobInitializationData.jobPath.name,
                jobIdentifier,
                [] as String[] /* converter above takes care of params */,
                (
                        [] +
                                (opts.r ? '-restart' : []) +
                                (opts.s ? '-stop' : []) +
                                (opts.a ? '-abandon' : []) +
                                (opts.n ? '-next' : [])) as Set)
    }

    private static JobParametersConverter getJobParametersConverterForJobDetails(final JobStartupDetails jobDetails) {
        new JobParametersConverter() {
            @SuppressWarnings('UnusedMethodParameter')
            JobParameters getJobParameters(Properties properties) {
                jobDetails as JobParameters
            }

            @SuppressWarnings('UnusedMethodParameter')
            Properties getProperties(JobParameters params) {
                jobDetails as Properties
            }
        }
    }

    String getBatchPropertiesPath() {
        if (!opts.c) {
            DEFAULT_BATCHDB_PROPERTIES_LOCATION
        } else {
            Path path = Paths.get((String) opts.c)
            if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
                throw new InvalidParametersFileException("'$path' is not a readable file")

            }
            "file:${path.toAbsolutePath()}"
        }
    }

    List<Path> getParamsFilePaths() {
        if (!opts.ps) {
            return []
        }
        List<Path> paths = opts.ps.collect { Paths.get(it) }
        paths.each { path ->
            if (!Files.isReadable(path) || !Files.isRegularFile(path)) {
                throw new InvalidParametersFileException("'$path' is not a readable file")
            }
        }
        paths
    }

    List<Path> getParamsFolderPaths() {
        if (!opts.fs) {
            return []
        }
        List<Path> paths = opts.fs.collect { Paths.get(it) }
        paths.each { path ->
            if (!Files.isReadable(path) || !Files.isDirectory(path)) {
                throw new InvalidParametersFileException("'$path' is not a readable folder")
            }
        }
        paths
    }

    String calculateJobIdentifier(Class configurationClass) {
        /* CommandLineJobRunner uses -j both for a bean job name
         * (or logical job name to be found by a JobLocator) or
         * a JobInstance identifier or name.
         *
         * The job name should also match its bean name because
         * on restarts the job name saved in the registry is
         * used as the bean name
         */
        if (opts.r || opts.s || opts.a) {
            if (!opts.j) {
                throw new IllegalStateException('The -j parameter must be specified ' +
                        'when the options -n, -s or -a are used')
            } else {
                opts.j
            }
        } else {
            def job = configurationClass.JOB_NAME
            if (!job) {
                throw new IllegalStateException("Class $configurationClass should " +
                        "have a static property 'JOB_NAME'")
            }

            job
        }
    }

    List<JobStartupDetails> getJobsStartupDetails() {
        List<JobStartupDetails> result = []
        Map<String, String> paramOverrides = parametersOverrides
        List<Path> paramsFiles = paramsFilePaths
        List<Path> paramsFolders = paramsFolderPaths

        try {
            result.addAll paramsFiles.collect { paramsFile ->
                JobStartupDetails.fromFile(paramsFile, paramOverrides)
            }
            result.addAll paramsFolders.collectMany { paramsFolder ->
                JobStartupDetails.fromFolder(paramsFolder, paramOverrides)
            }

            result.sort().unique()
        } catch (InvalidParametersFileException e) {
            throw new InvalidParametersFileException(e)
        }
    }

    private Map<String, String> getParametersOverrides() {
        def ds = opts.ds
        Map<String, String> paramOverrides = [:]
        if (ds) {
            for (int i = 0; i < ds.size(); i += 2) {
                paramOverrides[ds[i]] = ds[i + 1]
            }
        }

        paramOverrides
    }
}
