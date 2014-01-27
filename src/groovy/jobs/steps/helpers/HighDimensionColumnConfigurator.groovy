package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.HighDimensionMultipleRowsResultColumn
import jobs.table.columns.HighDimensionSingleRowResultColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException

import static jobs.steps.OpenHighDimensionalDataStep.createConceptKeyFrom

@Component
@Scope('prototype')
class HighDimensionColumnConfigurator extends ColumnConfigurator {

    String columnHeader

    String projection

    String keyForConceptPath,
           keyForDataType,
           keyForSearchKeywordId

    boolean multiRow = false

    @Autowired
    private HighDimensionResource highDimensionResource

    @Autowired
    private ResultInstanceIdsHolder resultInstanceIdsHolder

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        String dataType = getStringParam(keyForDataType)
        HighDimensionDataTypeResource subResource =
                highDimensionResource.getSubResourceForType dataType

        def projection = subResource.createProjection [:], projection
        def assayConstraints = []

        assayConstraints << subResource.createAssayConstraint(
                AssayConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (AssayConstraint.PATIENT_SET_CONSTRAINT):
                                resultInstanceIdsHolder.resultInstanceIds.collect {
                                    [result_instance_id: it]
                                }])
        assayConstraints << subResource.createAssayConstraint(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                concept_key: createConceptKeyFrom(getStringParam(keyForConceptPath)))

        def searchKeyword = getStringParam(keyForSearchKeywordId)
        if (!searchKeyword.isLong()) {
            throw new InvalidArgumentsException("Illegal search keyword id: $searchKeyword")
        }
        def dataConstraint = subResource.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                keyword_ids: [searchKeyword])

        def tabularResult = subResource.retrieveData(
                assayConstraints, [dataConstraint], projection)

        String dataSourceName = columnHeader + '_highdim'

        table.addDataSource dataSourceName, tabularResult

        def highDimColumn
        if (!multiRow) {
            highDimColumn = new HighDimensionSingleRowResultColumn(
                    header: columnHeader)
        } else {
            highDimColumn = new HighDimensionMultipleRowsResultColumn(
                    header: columnHeader)
        }
        table.addColumn(
                decorateColumn.call(highDimColumn),
                [dataSourceName] as Set)
    }
}
