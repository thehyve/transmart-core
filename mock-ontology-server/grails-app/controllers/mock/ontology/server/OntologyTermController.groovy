package mock.ontology.server

import grails.converters.*
import sun.reflect.generics.reflectiveObjects.NotImplementedException

class OntologyTermController {
    static responseFormats = ['json', 'xml']
    public static final Boolean USE_EXTERNAL_SERVER_RESPONSE = false
    public static final int RESPONSE_SIZE = 3

    def index() {
        def concepts = fetchPreferredConcept(params.conceptCode)
        render concepts as JSON
    }

    private List<String> fetchPreferredConcept(String conceptCode) {
        if(USE_EXTERNAL_SERVER_RESPONSE) {
            return getResponseFromExternalServer(conceptCode)
        }
        else{
            return (1..RESPONSE_SIZE).collect{"$conceptCode recommended_$it"}

        }
    }

    private List<String> getResponseFromExternalServer(String conceptCode) {
        throw new NotImplementedException()
        //TODO: use for example http://sparql.bioontology.org/examples to get more realistic data
    }
}
