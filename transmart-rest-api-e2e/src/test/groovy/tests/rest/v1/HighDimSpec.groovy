package tests.rest.v1

import base.RESTSpec

import org.apache.http.conn.EofSensorInputStream
import spock.lang.Requires

import static config.Config.CELL_LINE_ID
import static config.Config.CELL_LINE_LOADED
import static config.Config.V1_PATH_STUDIES

@Requires({CELL_LINE_LOADED})
class HighDimSpec extends RESTSpec{

    /**
     *  given: "study CELL-LINE is loaded"
     *  when: "I request all highdim dataTypes for a concept"
     *  then: "I get all relevant dataTypes"
     */
    def "v1 highdim dataTypes"(){
        given: "study CELL-LINE is loaded"

        def studieId = CELL_LINE_ID
        def conceptPath = 'Molecular profiling/High-throughput molecular profiling/Expression (miRNA)/Agilent miRNA microarray/Gene level/Normalised ratios/'

        when: "I request all highdim dataTypes for a concept"
        def responseData = get([path: V1_PATH_STUDIES+"/${studieId}/concepts/${conceptPath}/highdim", acceptType: contentTypeForoctetStream])

        then: "I get all relevant dataTypes"
        assert responseData.dataTypes.each {
            assert it.assayCount == 2
            assert it.name == 'mirnaqpcr'
            assert it.supportedAssayConstraints == ['patient_set', 'disjunction', 'assay_id_list', 'patient_id_list', 'ontology_term', 'trial_name']
            assert it.supportedDataConstraints == ['annotation', 'search_keyword_ids', 'disjunction', 'mirnas']
            assert it.supportedProjections == ['default_real_projection', 'zscore', 'log_intensity', 'all_data']
        }
    }

    /**
     *  given: "study CELL-LINE is loaded"
     *  when: "I request highdim data"
     *  then: "I get a file stream"
     */
    def "v1 single "(){
        given: "study CELL-LINE is loaded"
        def studieId = CELL_LINE_ID
        def conceptPath = 'Molecular profiling/High-throughput molecular profiling/Expression (miRNA)/Agilent miRNA microarray/Gene level/Normalised ratios/'

        when: "I request highdim data"
        def responseData = get([
                path: V1_PATH_STUDIES+"/${studieId}/concepts/${conceptPath}/highdim",
                query: [
                        dataType: 'mirnaqpcr',
                        projection: 'zscore'
                ],
                acceptType: contentTypeForoctetStream])

        then: "I get a file stream"
        assert responseData.getClass() == EofSensorInputStream.class
    }
}
