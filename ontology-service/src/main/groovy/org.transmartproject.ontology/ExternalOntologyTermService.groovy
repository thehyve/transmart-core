package org.transmartproject.ontology

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.HTTPBuilder

/**
 * Created by ewelina on 7-12-16.
 */
@Slf4j
class ExternalOntologyTermService {

    //TODO: move configuration parameters to config file
    public static final String ONTOLOGY_SERVER_URL = 'http://localhost:8081/'
    public static final String SEARCH_TEXT_PATH = "search/"
    def contentType = 'application/json'
    private http = new HTTPBuilder(ONTOLOGY_SERVER_URL)

    public Object fetchPreferredConcept(String conceptCode) {
        // TODO: Add possibility to use other parameters (optional), decide on which we want to use
        def responseData = get("$SEARCH_TEXT_PATH/$conceptCode", contentType)
        wrapOntologyServerResponse(responseData)
    }

    /**
     * http request to get preferred concept codes fetched from external ontology server
     *
     * @param path
     * @param AcceptHeader
     * @return
     */
    def get(String path, String AcceptHeader){
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

    private Object wrapOntologyServerResponse(responseData)
    {
        def recommendedValues = getHighlyReccomendedValues(responseData)
        def conceptPaths = recommendedValues.classpath.collect{ path->
            path.join("/");
        }
        def label = recommendedValues.label
        [
                concept_paths : conceptPaths,
                label : label
        ]
    }

    private Object getHighlyReccomendedValues(responseData){
        //TODO decide on which value is the most recommended one
        responseData.get(0)
    }
}
