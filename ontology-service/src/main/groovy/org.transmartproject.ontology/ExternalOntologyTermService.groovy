package org.transmartproject.ontology

import au.com.bytecode.opencsv.CSVWriter
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.HTTPBuilder

/**
*  Created by ewelina on 7-12-16.
*/
@Slf4j
class ExternalOntologyTermService {

    /**
     * Ontology service for fetching ontology metadata for a concept code.
     * @param ontologyServerUrl
     * @param searchTextRequestPath
     * @param conceptCodeDetailsRequestPath
     */
    ExternalOntologyTermService(ontologyServerUrl, searchTextRequestPath, conceptCodeDetailsRequestPath) {
        this.ONTOLOGY_SERVER_URL = ontologyServerUrl
        this.SEARCH_TEXT_REQUEST_PATH = searchTextRequestPath
        this.CONCEPT_CODE_DETAILS_REQUEST_PATH = conceptCodeDetailsRequestPath
        this.http = new HTTPBuilder(ONTOLOGY_SERVER_URL)
    }

    def ONTOLOGY_SERVER_URL // = 'http://localhost:8081/'
    def SEARCH_TEXT_REQUEST_PATH // = 'search/'
    def CONCEPT_CODE_DETAILS_REQUEST_PATH // = ''
    private http

    private String contentType = 'application/json'
    private String categoryCode
    private String dataLabel

    /**
     * Reference terms from external ontology servers
     * @param categoryCode
     * @param dataLabel
     * @return
     */
    public Object fetchPreferredConcept(String categoryCode, String dataLabel) {
        this.categoryCode = categoryCode
        this.dataLabel = dataLabel

        def responseData = get("$SEARCH_TEXT_REQUEST_PATH/$categoryCode", contentType)

        if(responseData.size() > 0) {
            def recommendedValues = getHighlyRecommendedValues(responseData)
            LinkedHashMap responseMap = wrapOntologyServerResponse(recommendedValues.classpath)
            List values = new ArrayList()
            LinkedHashMap labels = getLabels(responseMap.keySet())
            Iterator iterator = responseMap.iterator()

            String rootValue = iterator.next().key
            String rootLabel = recommendedValues.label

            responseMap.each {
                OntologyMap ontologyMap = values ? mapResponseToOntologyCodes(it, labels[it.key]) : mapResponseToOntologyCodes(it, rootLabel, rootValue )
                String[] value = [ontologyMap.categoryCode,
                                  ontologyMap.dataLabel,
                                  ontologyMap.ontologyCode,
                                  ontologyMap.label,
                                  ontologyMap.uri,
                                  ontologyMap.ancestors]
                values.add(value)
            }
            return generateTSV(values)
        }
        else return null
    }

    LinkedHashMap getLabels(Collection values) {
        def labelMap = [:]
        values.collect{
            def detail = get("$CONCEPT_CODE_DETAILS_REQUEST_PATH/$it", contentType)
            labelMap.put(it, detail.node)
        }
        labelMap
    }
/**
     * Http request to get preferred concept codes fetched from external ontology server
     * @param path
     * @param AcceptHeader
     * @return
     */
    def get(String path, String AcceptHeader) {
        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = path
            headers.Accept = AcceptHeader

            log.info(URLDecoder.decode(uri.toString(), 'UTF-8'))
            response.success = { resp, reader ->
                if(!resp.headers.'Content-Type'.contains(AcceptHeader))
                    log.error("Response was successful but not what was expected.")
                log.info("Got response: ${resp.statusLine}")
                log.info("Content-Type: ${resp.headers.'Content-Type'}")
                return reader
            }

            response.failure = { resp, reader ->
                log.error(resp.statusLine.statusCode)
                log.error( "Got response: ${resp.statusLine}")
                log.error( "Content-Type: ${resp.headers.'Content-Type'}")
                return reader
            }
        }
    }

    /**
     * Wrap server response into ONTOLOGY_MAP_FILE table format.
     * @param paths
     * @return
     */
    private LinkedHashMap wrapOntologyServerResponse(paths) {
        def multimap = [:]
        paths.each { path ->
            for(int i = 0; i < path.size(); i++) {
                if (multimap[path[i]] == null) {
                    multimap[path[i]] = new HashSet()
                }
                if (i > 0) {
                    multimap[path[i]] << path[i - 1]
                }
            }
        }
        multimap
    }

    /**
     * Get most preferred codes from list of retrieved values.
     * @param responseData
     * @return
     */
    private Object getHighlyRecommendedValues(responseData) {
        //TODO decide on which value is the most recommended one
        responseData.get(0)
    }

    /**
     * Create OntologyMap object from external server response.
     * @param response
     * @param rootValue
     * @param label
     * @return
     */
    private Object mapResponseToOntologyCodes(response, String label = "", String rootValue = null) {
        if (rootValue) {
            OntologyMap ontologyMap = new OntologyMap(
                    categoryCode,
                    dataLabel,
                    response.key,
                    label,
                    "$ONTOLOGY_SERVER_URL$SEARCH_TEXT_REQUEST_PATH$categoryCode",
                    ""
            )
            return ontologyMap
        } else {
            OntologyMap ontologyMap = new OntologyMap(
                    response.key,
                    label,
                    "$ONTOLOGY_SERVER_URL$SEARCH_TEXT_REQUEST_PATH$categoryCode",
                    response.value.join(", ")
            )
            return ontologyMap
        }
    }

    /**
     * Convert results into TSV file format.
     * @param values
     * @return
     */
    private static StringWriter generateTSV(List<String[]> values) {
        StringWriter buffer = new StringWriter()
        CSVWriter writer = new CSVWriter(buffer, '\t' as char)
        try {
            writer.writeNext(OntologyMap.categoryCodeHeader,
                    OntologyMap.dataLabelHeader,
                    OntologyMap.ontologyCodeHeader,
                    OntologyMap.labelHeader,
                    OntologyMap.uriHeader,
                    OntologyMap.ancestorsHeader)
            values.each {
                writer.writeNext(it)
            }
        } finally {
            writer.close()
        }
        buffer
    }
}
