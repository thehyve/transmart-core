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

        //if required this will fail on empty conceptPaths
        String conceptPaths = getStringParam(keyForConceptPaths, required)

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
}
