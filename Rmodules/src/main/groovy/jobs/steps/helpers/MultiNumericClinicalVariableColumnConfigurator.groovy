package jobs.steps.helpers

import com.recomdata.transmart.util.Functions
import jobs.table.Column
import jobs.table.columns.MultiNumericClinicalVariableColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariableColumn
import org.transmartproject.utils.ConceptUtils

@Component
@Scope('prototype')
class MultiNumericClinicalVariableColumnConfigurator extends ColumnConfigurator {

    String keyForConceptPaths

    GroupNamesHolder groupNamesHolder

    @Autowired
    private ClinicalDataRetriever clinicalDataRetriever

    @Autowired
    private ResultInstanceIdsHolder resultInstanceIdsHolder

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {

        List<String> variables = getConceptPaths().collect {
            clinicalDataRetriever.createVariableFromConceptPath it
        }

        variables = variables.collect {
            clinicalDataRetriever << it
        }

        clinicalDataRetriever.attachToTable table

        Map<ClinicalVariableColumn, String> variableToGroupName =
                Functions.inner(variables,
                        conceptPaths,
                        { a, b -> [a,b]}).
                        collectEntries()

        if (groupNamesHolder) {
            groupNamesHolder.groupNames = variableToGroupName.values() as List
        }

        table.addColumn(
                decorateColumn.call(
                        new MultiNumericClinicalVariableColumn(
                                clinicalVariables: variableToGroupName,
                                header:            header)),
                [ClinicalDataRetriever.DATA_SOURCE_NAME] as Set)
    }

    public List<String> getConceptPaths() {
        getStringParam(keyForConceptPaths).split(/\|/) as List
    }

}
