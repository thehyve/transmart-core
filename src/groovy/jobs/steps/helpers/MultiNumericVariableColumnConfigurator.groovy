package jobs.steps.helpers

import com.recomdata.transmart.util.Functions
import jobs.table.Column
import jobs.table.columns.MultiNumericClinicalVariableColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn

@Component
@Scope('prototype')
class MultiNumericVariableColumnConfigurator extends ColumnConfigurator {

    String columnHeader

    String keyForConceptPaths

    @Autowired
    private ClinicalDataRetriever clinicalDataRetriever

    @Autowired
    private ResultInstanceIdsHolder resultInstanceIdsHolder

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        List<String> conceptPaths =
                getStringParam(keyForConceptPaths).split(/\|/) as List

        List<ClinicalVariable> variables = conceptPaths.collect {
            clinicalDataRetriever.createVariableFromConceptPath it
        }

        variables.each {
            clinicalDataRetriever << it
        }

        clinicalDataRetriever.attachToTable table

        Map<ClinicalVariableColumn, String> variableToGroupName =
                Functions.inner(variables,
                        conceptPaths.collect { generateGroupName it },
                        { a, b -> [a,b]}).
                        collectEntries()

        table.addColumn(
                decorateColumn.call(
                        new MultiNumericClinicalVariableColumn(
                                clinicalVariables: variableToGroupName,
                                header:            columnHeader)),
                [ClinicalDataRetriever.DATA_SOURCE_NAME] as Set)
    }

    private String generateGroupName(String conceptPath) {
        /* find last non-empty segment (separated by \) */
        conceptPath.split('\\\\').findAll()[-1]
    }
}
