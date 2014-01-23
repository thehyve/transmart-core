package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.CategoricalVariableColumn
import jobs.table.columns.ConstantValueColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariable

@Component
@Scope('prototype')
class CategoricalColumnConfigurator extends ColumnConfigurator {

    String columnHeader

    String keyForConceptPaths

    @Autowired
    private ClinicalDataRetriever clinicalDataRetriever

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {

        String conceptPaths = getConceptPaths()

        if (conceptPaths != '') {
            Set<ClinicalVariable> variables =
                    conceptPaths.split(/\|/).collect {
                        clinicalDataRetriever.createVariableFromConceptPath it.trim()
                    }

            variables.each {
                clinicalDataRetriever << it
            }

            clinicalDataRetriever.attachToTable table

            table.addColumn(
                    decorateColumn.call(
                            new CategoricalVariableColumn(
                                    header: columnHeader,
                                    leafNodes: variables)),
                    [ClinicalDataRetriever.DATA_SOURCE_NAME] as Set)
        } else {
            //optional, empty value column
            table.addColumn(new ConstantValueColumn(header: columnHeader, missingValueAction: missingValueAction), Collections.emptySet())
        }
    }

    String getConceptPaths() {
        //if required this will fail on empty conceptPaths
        getStringParam(keyForConceptPaths, required)
    }
}
