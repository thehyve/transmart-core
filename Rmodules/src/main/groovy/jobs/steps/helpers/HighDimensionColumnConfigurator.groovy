package jobs.steps.helpers

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
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

import static jobs.misc.Hacks.createConceptKeyFrom

/**
 * A configurator supporting one or more high dimensional nodes. Multiple
 * concepts are only accepted if <code>multiConcepts</code> is
 * <code>true</code>. In that case a column will be configured that stores
 * &lt;concept path>|&lt;row label> as context.
 */
@Component
@Scope('prototype')
class HighDimensionColumnConfigurator extends ColumnConfigurator {

    String projection

    String keyForConceptPath,
           keyForDataType,
           keyForSearchKeywordId

    boolean multiRow = false

    // multiConcepts => multiRow
    boolean multiConcepts = false

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
        def searchKeywordIds = getStringParam(keyForSearchKeywordId).split(',') as List
        def illegalSearchKeywordIds = searchKeywordIds.findAll{ !it.isLong() }
        if (illegalSearchKeywordIds) {
            throw new InvalidArgumentsException("Illegal search keyword ids: ${illegalSearchKeywordIds.join(',')}")
        }

        [subResource.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                keyword_ids: searchKeywordIds)]
    }()

    private TabularResult<AssayColumn, Number> openResultSet(String conceptPath) {
        def assayConstraints = [patientSetConstraint]
        assayConstraints << subResource.createAssayConstraint(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                concept_key: createConceptKeyFrom(conceptPath))

        subResource.retrieveData assayConstraints, dataConstraints, createdProjection
    }

    private Map<String, TabularResult> openMultipleResultSets() {
        assert multiConcepts

         conceptPaths.collectEntries {
            [it, openResultSet(it)]
        }
    }

    private Set<String> findCommonPatients(Map<String, TabularResult> results) {
        Map<TabularResult, List<AssayColumn>> mapOfAssayLists =
                results.collectEntries { String conceptPath,
                                         TabularResult result ->
                    [result, result.indicesList]
                }

        List<Set<String>> patientsSets = mapOfAssayLists.values().
                collect { List<AssayColumn> assays ->
                    Sets.newHashSet assays*.patientInTrialId
                }

        Set<String> commonPatients = patientsSets[0]
        if (patientsSets.size() > 1) {
            patientsSets[1..-1].each { Set<String> current ->
                commonPatients = Sets.intersection commonPatients, current
            }
        }

        if (commonPatients.empty) {
            throw new InvalidArgumentsException(
                    "The intersection of the patients for the assays of the " +
                            "${commonPatients.size()} result sets is empty. " +
                            "The patient sets for each result are, in order: " +
                            patientsSets
            )
        }

        commonPatients
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        if (conceptPaths.size() == 0) {
            throw new InvalidArgumentsException(
                    "Found empty concept paths list (key $keyForConceptPath)")
        }

        if (!header) {
            throw new IllegalStateException('header property must be set')
        }

        Map<String, TabularResult> tabularResults
        Set<String> commonPatients
        if (conceptPaths.size() == 1 && !multiConcepts) {
            tabularResults = ImmutableMap.of(
                    header + '_highdim',
                    openResultSet(conceptPaths[0]))
        } else {
            if (!multiConcepts) {
                throw new InvalidArgumentsException(
                        "Got multiple concept paths (key $keyForConceptPath), " +
                                "but multiConcepts is not on")
            }

            tabularResults = openMultipleResultSets()
            commonPatients = findCommonPatients(tabularResults)
        }

        tabularResults.each { String dataSourceName,
                              TabularResult dataSource ->
            table.addDataSource dataSourceName, dataSource
        }

        def highDimColumn
        if (!multiRow) {
            highDimColumn = new HighDimensionSingleRowResultColumn(
                    header: header)
        } else {
            highDimColumn = new HighDimensionMultipleRowsResultColumn(
                    header:             header,
                    patientsToConsider: commonPatients)
        }
        table.addColumn(
                decorateColumn.call(highDimColumn),
                tabularResults.keySet())
    }
}
