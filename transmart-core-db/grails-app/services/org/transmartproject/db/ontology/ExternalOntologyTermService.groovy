package org.transmartproject.db.ontology

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import org.transmartproject.core.ontology.ExternalOntologyTerm
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator

/**
 * Created by ewelina on 7-12-16.
 */
class ExternalOntologyTermService implements ExternalOntologyTerm {

    //TODO: move configuration parameters to config file
    public static final String ONTOLOGY_SERVER_URL = 'http://localhost:8081/'
    public static final String SEARCH_TEXT_PATH = "search/"
    def contentType = 'application/json'
    private http = new HTTPBuilder(ONTOLOGY_SERVER_URL)

    @Override
    List<String> fetchPreferredConcept(String conceptCode) {
        // TODO: Add possibility to use other parameters (optional), decide on which we want to use
        def responseData = get("$SEARCH_TEXT_PATH/$conceptCode", contentType)
        responseData
    }

    /**
     * http request to get preferred concept codes fetched from external ontology server
     *
     * @param path
     * @param AcceptHeader
     * @param query
     * @return
     */
    def get(String path, String AcceptHeader){
        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = path
            headers.Accept = AcceptHeader

            log.info(URLDecoder.decode(uri.toString(), 'UTF-8'))
            response.success = { resp, reader ->
                if(resp.headers.'Content-Type'.contains(AcceptHeader))
                    log.error("Response was successful but not what was expected.")
                log.info("Got response: ${resp.statusLine}")
                log.info("Content-Type: ${resp.headers.'Content-Type'}")
                log.info(reader)
                return reader
            }

            response.failure = { resp, reader ->
                log.error(resp.statusLine.statusCode)
                log.error( "Got response: ${resp.statusLine}")
                log.error( "Content-Type: ${resp.headers.'Content-Type'}")
                log.error( reader)
                return reader
            }
        }
    }
}
