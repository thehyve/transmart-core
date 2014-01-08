package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.CategoricalVariableColumn
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
        Set<ClinicalVariable> variables =
                getStringParam(keyForConceptPaths).split(/\|/).collect {
                    clinicalDataRetriever.createVariableFromConceptPath it
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
    }
}
