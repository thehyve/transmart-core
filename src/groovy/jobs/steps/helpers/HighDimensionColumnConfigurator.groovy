package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.HighDimensionMultipleRowsResultColumn
import jobs.table.columns.HighDimensionSingleRowResultColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException

import static jobs.steps.OpenHighDimensionalDataStep.createConceptKeyFrom

/**
 * A configurator supporting one or more high dimensional nodes. Multiple
 * concepts are only accepted if <code>multiConcepts</code> is
 * <code>true</code>. In that case a column will be configured that stores
 * &lt;concept path>|&lt;row label> as context.
 */
@Component
@Scope('prototype')
class HighDimensionColumnConfigurator extends ColumnConfigurator {

    String columnHeader

    String projection

    String keyForConceptPath,
           keyForDataType,
           keyForSearchKeywordId

    boolean multiRow = false

    // multiConcepts => multiRow
    boolean multiConcepts = false

    // not implemented
    //boolean pruneConceptPath = false

    boolean isMultiConcepts() {
        multiConcepts
    }

    boolean isMultiRow() {
        multiRow || multiConcepts
    }

    @Autowired
    private HighDimensionResource highDimensionResource

    @Autowired
    private ResultInstanceIdsHolder resultInstanceIdsHolder

    @Lazy private List<String> conceptPaths = {
        getStringParam(keyForConceptPath).split(/\|/) as List
    }()

    @Lazy private HighDimensionDataTypeResource subResource = {
        String dataType = getStringParam(keyForDataType)
        highDimensionResource.getSubResourceForType dataType
    }()

    @Lazy private Projection createdProjection = {
        subResource.createProjection [:], projection
    }()

    @Lazy private AssayConstraint patientSetConstraint = {
        subResource.createAssayConstraint(
                AssayConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (AssayConstraint.PATIENT_SET_CONSTRAINT):
                                resultInstanceIdsHolder.resultInstanceIds.collect {
                                    [result_instance_id: it]
                                }])
    }()

    @Lazy private List<DataConstraint> dataConstraints = {
        def searchKeyword = getStringParam(keyForSearchKeywordId)
        if (!searchKeyword.isLong()) {
            throw new InvalidArgumentsException("Illegal search keyword id: $searchKeyword")
        }

        [subResource.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                keyword_ids: [searchKeyword])]
    }()

    TabularResult<AssayColumn, Number> openResultSet(String conceptPath) {
        def assayConstraints = [patientSetConstraint]
        assayConstraints << subResource.createAssayConstraint(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                concept_key: createConceptKeyFrom(conceptPath))

        subResource.retrieveData assayConstraints, dataConstraints, createdProjection
    }

    TabularResult<AssayColumn, Number> createCompositeResultSet() {
        assert multiConcepts

        def tabularResults = conceptPaths.collectEntries {
            [it, openResultSet(it)]
        }

        new CompositeTabularResult(results: tabularResults)
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        if (conceptPaths.size() == 0) {
            throw new InvalidArgumentsException(
                    "Found empty concept paths list (key $keyForConceptPath)")
        }

        TabularResult<AssayColumn, Number> tabularResult
        if (conceptPaths.size() == 1) {
            tabularResult = openResultSet conceptPaths[0]
        } else {
            if (!multiConcepts) {
                throw new InvalidArgumentsException(
                        "Got multiple concept paths (key $keyForConceptPath), " +
                                "but multiConcepts is not on")
            }

            tabularResult = createCompositeResultSet()
        }

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
