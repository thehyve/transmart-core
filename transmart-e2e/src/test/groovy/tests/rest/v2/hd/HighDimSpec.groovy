package tests.rest.v2.hd

import base.RESTSpec
import groovy.json.JsonBuilder

import static tests.rest.v2.constraints.BiomarkerConstraint
import static tests.rest.v2.constraints.ConceptConstraint

class HighDimSpec extends RESTSpec {

    //see CLINICAL_TRIAL_HIGHDIM, EHR_HIGHDIM and TUMOR_NORMAL_SAMPLES studies at the transmart-data/test_data/studies folder

    def "example call that shows all supported parameters"() {
        // any of supported constraints (but biomarker constraint) and their combinations.
        def assayConstraint = [
                type: ConceptConstraint,
                path: "\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\"
        ]
        // see transmart-core-api/src/main/groovy/org/transmartproject/core/dataquery/highdim/dataconstraints/DataConstraint.groovy
        def biomarkerConstraint = [
                type: BiomarkerConstraint,
                biomarkerType: 'genes',
                params: [
                        names: ['TP53']
                ]
        ]
        // see transmart-core-api/src/main/groovy/org/transmartproject/core/dataquery/highdim/projections/Projection.groovy
        def projection = 'all_data'

        when:
        def responseData = get("query/highDim", contentTypeForJSON, [
                assay_constraint: new JsonBuilder(assayConstraint),
                biomarker_constraint: new JsonBuilder(biomarkerConstraint),
                projection: projection

        ])

        then:
        def biomarkers = 2
        def assays = 6
        def projections = 10
        def metaRows = 2
        responseData.size() == biomarkers * assays * projections + metaRows
    }

}
