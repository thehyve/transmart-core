package jobs.steps

import jobs.misc.AnalysisConstraints
import jobs.UserParameters
import jobs.misc.Hacks
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

class OpenHighDimensionalDataStep implements Step {

    final String statusName = 'Gathering Data'

    /* in */
    UserParameters params
    HighDimensionDataTypeResource dataTypeResource
    AnalysisConstraints analysisConstraints

    /* out */
    Map<List<String>, TabularResult> results = [:]

    @Override
    void execute() {
        try {
            List<String> ontologyTerms = extractOntologyTerms()
            extractPatientSets().eachWithIndex { resultInstanceId, index ->
                ontologyTerms.each { ontologyTerm ->
                    String seriesLabel = ontologyTerm.split('\\\\')[-1]
                    List<String> keyList = ["S" + (index + 1), seriesLabel]
                    results[keyList] = fetchSubset(resultInstanceId, ontologyTerm)
                }
            }
        } catch(Throwable t) {
            results.values().each { it.close() }
            throw t
        }
    }

    private List<String> extractOntologyTerms() {
        analysisConstraints.assayConstraints.remove('ontology_term').collect {
            Hacks.createConceptKeyFrom(it.term)
        }
    }

    private List<Integer> extractPatientSets() {
        analysisConstraints.assayConstraints.remove("patient_set").grep()
    }

    private TabularResult fetchSubset(Integer patientSetId, String ontologyTerm) {

        List<DataConstraint> dataConstraints = analysisConstraints['dataConstraints'].
                collect { String constraintType, values ->
                    if (values) {
                        dataTypeResource.createDataConstraint(values, constraintType)
                    }
                }.grep()

        List<AssayConstraint> assayConstraints = analysisConstraints['assayConstraints'].
                collect { String constraintType, values ->
                    if (values) {
                        dataTypeResource.createAssayConstraint(values, constraintType)
                    }
                }.grep()

        assayConstraints.add(
                dataTypeResource.createAssayConstraint(
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: patientSetId))

        assayConstraints.add(
                dataTypeResource.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                        concept_key: ontologyTerm))

        Projection projection = dataTypeResource.createProjection([:],
                analysisConstraints['projections'][0])

        dataTypeResource.retrieveData(assayConstraints, dataConstraints, projection)
    }
}
