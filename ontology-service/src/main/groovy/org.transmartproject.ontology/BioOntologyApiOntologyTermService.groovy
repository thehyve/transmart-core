/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.ontology

import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method

/**
 * Ontology service for fetching ontology metadata for a concepts using
 * the API from {@link https://bioportal.bioontology.org}.
 * Docmentation is available at {@link https://data.bioontology.org/documentation}.
 */
@Slf4j
class BioOntologyApiOntologyTermService implements ExternalOntologyTermService {

    final static String name = "bioontology"

    private static final ContentType CONTENT_TYPE = ContentType.JSON

    private String ontologyServerUrl = 'https://data.bioontology.org'
    private String ontologyServerApiKeyToken = null
    private String ontologyServerOntologies = 'SNOMEDCT'
    private HTTPBuilder http

    private final Map<String, OntologyMap> conceptMap = [:]

    /**
     * Initialises the service.
     * Supported parameters:
     * - ontologyServerUrl
     * - ontologyServerApiKeyToken
     * - ontology
     */
    public void init(Map parameters) {
        ['ontologyServerUrl', 'ontologyServerApiKeyToken', 'ontology'].each { name ->
            def value = parameters[name]
            if (value) {
                this."${name}" = value as String
            }
        }
        this.http = new HTTPBuilder(ontologyServerUrl)
    }

    /**
     * Http request to get preferred concept codes fetched from external ontology server
     * @param path
     * @param acceptHeader
     * @return
     */
    def get(String path, Map query = null) {
        http.request(Method.GET, ContentType.JSON) { req ->
            if (query) {
                uri.path = path
                uri.query = query
            } else {
                uri = "${ontologyServerUrl}${path}"
            }
            headers['Accept'] = CONTENT_TYPE.acceptHeader
            if (ontologyServerApiKeyToken) {
                headers['Authorization'] = "apikey token=${ontologyServerApiKeyToken}"
            }

            log.debug "GET ${URLDecoder.decode(uri.toString(), 'UTF-8')}"
            response.success = { HttpResponseDecorator resp, Object data ->
                def contentType = (resp.headers.'Content-Type' as String).split(';')[0]
                if (!(contentType in CONTENT_TYPE.contentTypeStrings)) {
                    throw new OntologyServerConnectionException("" +
                            "Unsupported response type: ${contentType}")
                }
                return data
            }

            response.failure = { HttpResponseDecorator resp, reader ->
                if (resp.status == 404) {
                    return null
                }
                if (resp.status == 400) {
                    log.warn "Code ${resp.statusLine.statusCode}: ${resp.statusLine.reasonPhrase}"
                    resp.headers.each {
                        log.info "$it.name = $it.value"
                    }
                    return null
                }
                throw new OntologyServerConnectionException(
                        "Code ${resp.statusLine.statusCode}: ${resp.statusLine.reasonPhrase}")
            }
        }
    }

    static pattern = ~/(([^:]+):\/\/([^\/]+)\/ontology\/([^\/]+)\/(.+))/

    /**
     * Id is of the form http://purl.bioontology.org/ontology/SNOMEDCT/364072008.
     * Extracts the ontology name from the id.
     * @param id the ontology id in the uri form.
     */
    public static String getOntology(String id) {
        def m = pattern.matcher(id)
        if (m.find() && m.groupCount() == 5) {
            return m.group(4)
        }
        throw new RuntimeException("Could not find ontology in id '${id}'.")
    }

    /**
     * Id is of the form http://purl.bioontology.org/ontology/SNOMEDCT/364072008.
     * Extracts the ontology name and the code from the id, e.g., 'SNOMEDCT/364072008'.
     * @param id the ontology id in the uri form.
     * @return a string with ontology name and code.
     */
    public static String getCode(String id) {
        def m = pattern.matcher(id)
        if (m.find() && m.groupCount() == 5) {
            return "${m.group(4)}/${m.group(5)}"
        }
        throw new RuntimeException("Could not find code in id '${id}'.")
    }
    public OntologyMap createConcept(String id, String label, String dataLabel = null, String categoryCode = null) {
        new OntologyMap(
                categoryCode: categoryCode,
                dataLabel: dataLabel,
                ontologyCode: getCode(id),
                uri: id,
                label: label,
        )
    }

    /**
     * Fetch parent information for concept with id <code>id</code>.
     * Returns the list of concept codes of the parents. As a side effect,
     * ontology mapping items are added to the concept map, that is returned
     * by {@link #fetchPreferredConcept}.
     *
     * @param id the of the concept.
     * @return the list of concept codes of the parents.
     */
    public List<String> fetchAncestors(String id) {
        log.debug "Looking up parents of ${id} ..."
        def ontology = getOntology(id)
        def parentsLink = "/ontologies/${ontology}/classes/${URLEncoder.encode(id, 'utf-8')}/parents".toString()
        def response = get(parentsLink)
        def data = response as Collection
        if (!data || data.empty) {
           return []
        } else {
            def parentCodes = data.collect { parentData ->
                def parentId = parentData.'@id' as String
                log.debug "Found parent of ${id}: ${parentId}"
                def parent = conceptMap[parentId]
                if (!parent) {
                    def parentLabel = parentData.prefLabel as String
                    parent = createConcept(parentId, parentLabel)
                    conceptMap[parentId] = parent
                    conceptMap[parentId].ancestors = fetchAncestors(parentId)
                }
                getCode(parentId)
            }
            return parentCodes
        }
    }

    /**
     * Fetch preferred ontology terms from external ontology server.
     * The label <code>dataLabel</code> of a variable is used as search term.
     * When calling this method on one instance multiple times, all previously generated
     * terms are returned.
     *
     * @param categoryCode The category code of the original variable.
     * @param dataLabel The label of the original variable.
     * @return the list of ontology terms resulting from the ontology server, including terms
     * referenced as parents.
     */
    public List<OntologyMap> fetchPreferredConcept(String categoryCode, String dataLabel) {

        def query = ['q': dataLabel]
        if (ontologyServerOntologies) {
            query['ontologies'] = ontologyServerOntologies
        }
        def response = get('/search', query)

        if (!response) {
            return []
        }

        def data = response.collection as Collection

        if (!data || data.empty) {
            return []
        }

        def preferredConcept = data[0]

        def id = preferredConcept.'@id' as String
        def label = preferredConcept.prefLabel as String
        def concept = createConcept(id, label, dataLabel, categoryCode)
        conceptMap[id] = concept
        conceptMap[id].ancestors = fetchAncestors(id)

        conceptMap.values() as List<OntologyMap>
    }

}
