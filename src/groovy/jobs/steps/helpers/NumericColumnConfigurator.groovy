package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.HighDimensionMultipleRowsResultColumn
import jobs.table.columns.HighDimensionSingleRowResultColumn
import jobs.table.columns.SimpleConceptVariableColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException

import static jobs.steps.OpenHighDimensionalDataStep.createConceptKeyFrom

@Component
@Scope('prototype')
class NumericColumnConfigurator extends ColumnConfigurator {

    public static final String CLINICAL_DATA_TYPE_VALUE = 'CLINICAL'

    String columnHeader

    String projection

    String keyForConceptPath,
           keyForDataType,        /* CLINICAL for clinical data */
           keyForSearchKeywordId  /* only applicable for high dim data */

    boolean multiRow = false

    boolean alwaysClinical = false

    @Autowired
    private HighDimensionResource highDimensionResource

    @Autowired
    private ClinicalDataRetriever clinicalDataRetriever

    @Autowired
    private ResultInstanceIdsHolder resultInstanceIdsHolder

    @Override
    protected void doAddColumn(Closure<Column> columnDecorator) {
        if (isClinical()) {
            addColumnClinical columnDecorator
        } else {
            addColumnHighDim columnDecorator
        }
    }

    boolean isClinical() {
        return alwaysClinical || getStringParam(keyForDataType) == CLINICAL_DATA_TYPE_VALUE
    }

    private void addColumnHighDim(Closure<Column> decorateColumn) {
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

    private void addColumnClinical(Closure<Column> decorateColumn) {
        ClinicalVariable variable = clinicalDataRetriever.
                createVariableFromConceptPath getStringParam(keyForConceptPath).trim()
        clinicalDataRetriever << variable

        clinicalDataRetriever.attachToTable table

        table.addColumn(
                decorateColumn.call(
                        new SimpleConceptVariableColumn(
                                column:      variable,
                                numbersOnly: true,
                                header:      columnHeader)),
                [ClinicalDataRetriever.DATA_SOURCE_NAME] as Set)
    }

    /**
     * Sets parameter keys based on optional base key part
     * @param keyPart
     */
    void setKeys(String keyPart = '') {
        keyForConceptPath     = "${keyPart}Variable"
        keyForDataType        = "div${keyPart.capitalize()}VariableType"
        keyForSearchKeywordId = "div${keyPart.capitalize()}VariablePathway"
    }

}
