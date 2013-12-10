package jobs.steps

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection

import static jobs.AbstractAnalysisJob.RESULT_INSTANCE_IDS
import static jobs.AbstractAnalysisJob.SUBSET1
import static jobs.AbstractAnalysisJob.SUBSET2
import static jobs.AbstractAnalysisJob.PARAM_ANALYSIS_CONSTRAINTS

class OpenHighDimensionalDataStep implements Step {

    final String statusName = 'Gathering Data'

    /* in */
    Map<String, Object> params
    HighDimensionDataTypeResource dataTypeResource

    /* out */
    Map<String, TabularResult> results

    @Override
    void execute() {
        TabularResult subset1,
                      subset2
        try {
            subset1 = fetchSubset(RESULT_INSTANCE_IDS[SUBSET1])
            subset2 = fetchSubset(RESULT_INSTANCE_IDS[SUBSET2])
        } catch (Throwable t) {
            subset1?.close()
            throw t
        }

        results = [
                (SUBSET1) : subset1,
                (SUBSET2) : subset2,
        ]
    }

    private TabularResult fetchSubset(String subset) {
        if (params[subset] == null) {
            return
        }

        List<DataConstraint> dataConstraints = params[PARAM_ANALYSIS_CONSTRAINTS]['dataConstraints'].
                collect { String constraintType, values ->
                    if (values) {
                        dataTypeResource.createDataConstraint(values, constraintType)
                    }
                }.grep()

        List<AssayConstraint> assayConstraints = params[PARAM_ANALYSIS_CONSTRAINTS]['assayConstraints'].
                collect { String constraintType, values ->
                    if (values) {
                        dataTypeResource.createAssayConstraint(values, constraintType)
                    }
                }.grep()

        assayConstraints.add(
                dataTypeResource.createAssayConstraint(
                        //TODO: use the analysisConstraints
                        AssayConstraint.PATIENT_SET_CONSTRAINT,
                        result_instance_id: params[subset]))

        Projection projection = dataTypeResource.createProjection([:],
                params[PARAM_ANALYSIS_CONSTRAINTS]['projections'][0])

        dataTypeResource.retrieveData(assayConstraints, dataConstraints, projection)
    }
}
