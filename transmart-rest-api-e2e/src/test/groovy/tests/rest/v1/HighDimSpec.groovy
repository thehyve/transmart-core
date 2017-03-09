/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v1

import annotations.RequiresStudy
import base.RESTSpec
import org.apache.http.conn.EofSensorInputStream

import static base.ContentTypeFor.contentTypeForJSON
import static base.ContentTypeFor.contentTypeForoctetStream
import static config.Config.GSE8581_ID
import static config.Config.V1_PATH_STUDIES

@RequiresStudy(GSE8581_ID)
class HighDimSpec extends RESTSpec {

    /**
     *  given: "study CELL-LINE is loaded"
     *  when: "I request all highdim dataTypes for a concept"
     *  then: "I get all relevant dataTypes"
     */
    def "v1 highdim dataTypes"() {
        given: "study GSE8581 is loaded"

        def studieId = GSE8581_ID
        def conceptPath = 'Biomarker Data/Affymetrix Human Genome U133 Plus 2.0 Array/Lung/'

        when: "I request all highdim dataTypes for a concept"
        def responseData = get([path: V1_PATH_STUDIES + "/${studieId}/concepts/${conceptPath}/highdim", acceptType: contentTypeForJSON])

        then: "I get all relevant dataTypes"
        assert responseData.dataTypes.each {
            assert it.assayCount == 55
            assert it.name == 'mrna'
            assert it.supportedAssayConstraints == ['ontology_term', 'patient_set', 'disjunction', 'trial_name', 'assay_id_list', 'patient_id_list', 'concept_path']
            assert it.supportedDataConstraints == ['disjunction', 'genes', 'proteins', 'search_keyword_ids', 'gene_lists', 'gene_signatures', 'pathways', 'homologenes']
            assert it.supportedProjections == ['all_data', 'default_real_projection', 'zscore', 'log_intensity']
        }
    }

    /**
     *  given: "study GSE8581 is loaded"
     *  when: "I request highdim data"
     *  then: "I get a file stream"
     */
    def "v1 single "() {
        given: "study GSE8581 is loaded"
        def studieId = GSE8581_ID
        def conceptPath = 'Biomarker Data/Affymetrix Human Genome U133 Plus 2.0 Array/Lung/'

        when: "I request highdim data"
        def responseData = get([
                path      : V1_PATH_STUDIES + "/${studieId}/concepts/${conceptPath}/highdim",
                query     : [
                        dataType        : 'mrna',
                        projection      : 'default_real_projection',
                        assayConstraints: toJSON([patient_id_list: [ids: ["GSE8581GSM210196"]]])
                ],
                acceptType: contentTypeForoctetStream
        ])

        then: "I get a file stream"
        assert responseData.getClass() == EofSensorInputStream.class
    }
}
