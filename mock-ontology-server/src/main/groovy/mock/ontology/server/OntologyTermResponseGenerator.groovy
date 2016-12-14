package mock.ontology.server

import groovy.json.JsonBuilder
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Created by ewelina on 8-12-16.
 */
class OntologyTermResponseGenerator {

    public static final Boolean USE_EXTERNAL_SERVER_RESPONSE = false
    def masterTypeMasterTerm = [
            "ROX32425774572250970"
    ]
    def masterTypeTopConceptOfMasterTerm = [
            "ROX1305277804322",
    ]
    def masterTypeNarrowerConcept = [
            "ROX1305277804321",
            "ROX1366314046215",
            "ROX1305277804323",
            "ROX1381416546756",
            "ROX1394550342848",
            "ROX1305277804385",
            "ROX1305277804386",
            "ROX1423421881371",
            "ROX1394546352646"
    ]

    def hostedTypeMasterTerm = [
            "NCI",
    ]
    def hostedTypeTopConceptOfMasterTerm = [
            "NCIC1908",
    ]
    def hostedTypeNarrowerConcept = [
            "NCIC1932",
            "NCIC347",
            "NCIC1743",
            "NCIC45175",
            "NCIC45183",
            "NCIC45186",
            "NCIC45410",
            "NCIC44340",
            "NCIC45176",
            "NCIC45178",
            "NCIC29845"
    ]

    def labels = [
            "Adenosquamous Cell Lung Carcinoma [Preflabel,Altlabel,Definition in Indication]",
            "Clear Cell Lung Carcinoma [Preflabel,Altlabel in Indication]",
            "Large Cell Lung Carcinoma [Preflabel,Altlabel in Indication]",
            "b-Hexachlorocyclohexane [Definition in NCI]",
            "o-Aminoazotoluene [Definition in NCI]"
    ]

    /**
     * Create example of server response on /search/conceptCode call
     * @param idx
     * @return
     */
    public Object fetchPreferredConcepts(String conceptCode) {
        if (USE_EXTERNAL_SERVER_RESPONSE) {
            return getResponseFromExternalServer(conceptCode)
        } else {
            // TODO: for now response is the same for each conceptCode
            def elem = [
                    getResponseElement("130", labels[0], TerminologyType.master, getListOfMasterConcepts(3, 3)),
                    getResponseElement("130", labels[1], TerminologyType.master, getListOfMasterConcepts(3, 4)),
                    getResponseElement("70", labels[2], TerminologyType.master, getListOfMasterConcepts(3, 3)),
                    getResponseElement("16.5", labels[3], TerminologyType.hosted, getListOfHostedConcepts(1, 8)),
                    getResponseElement("16.5", labels[4], TerminologyType.hosted, getListOfHostedConcepts(1, 7))
            ]
            return elem
        }
    }

    private List<String> getResponseFromExternalServer(String conceptCode) {
        throw new NotImplementedException()
        //TODO: use for example http://sparql.bioontology.org/examples to get more realistic data
    }

    private Object getResponseElement(String score, String label, TerminologyType terminologyType,
                                      List classpath) {
        def element = [
                score           : score,
                label           : label,
                terminology_type: terminologyType.name(),
                classpath       : classpath
        ]
        element
    }

    private List getListOfMasterConcepts(int numOfLists, int numOfNarrowerCodesOnList) {
        def concepts = (1..numOfLists).withIndex().collect { elem, index ->
            List<String> codeList = new ArrayList<String>()
            codeList.add(masterTypeMasterTerm[0])
            codeList.add(masterTypeTopConceptOfMasterTerm[0])
            codeList.addAll(masterTypeNarrowerConcept.subList((1 + index), (numOfNarrowerCodesOnList + index)))
            codeList.add(masterTypeNarrowerConcept.last())
            elem = codeList
        }
        concepts
    }

    private List getListOfHostedConcepts(int numOfLists, int numOfNarrowerCodesOnList) {
        def concepts = (1..numOfLists).withIndex().collect { elem, index ->
            List<String> codeList = new ArrayList<String>()
            codeList.add(hostedTypeMasterTerm[0])
            codeList.add(hostedTypeTopConceptOfMasterTerm[0])
            codeList.addAll(hostedTypeNarrowerConcept.subList((1 + index), (numOfNarrowerCodesOnList + index)))
            codeList.add(hostedTypeNarrowerConcept.last())
            elem = codeList
        }
        concepts
    }

    def sampleDescriptionsMap = [
            "Indication"                       : "master terminology",
            "Cancer"                           : "top concept of master terminology",
            "Carcinoma"                        : "narrowed concept of 'Cancer'",
            "Adenosquamous Carcinoma"          : "narrower concept",
            "Ductal Carcinoma In Situ"         : "narrower concept",
            "Invasive Ductal Carcinoma"        : "narrower concept",
            "Invasive Lobular Carcinoma"       : "narrower concept",
            "Squamous cell carcinoma"          : "narrower concept",
            "Adenocarcinoma"                   : "narrower concept",
            "Anaplastic carcinoma"             : "narrower concept",
            "Large cell carcinoma"             : "narrower concept",
            "Small cell carcinoma"             : "narrower concept",
            "Basal cell carcinoma"             : "narrower concept",
            "Renal cell carcinoma"             : "narrower concept",
            "Adenosquamous Cell Lung Carcinoma": "narrower concept and search hit"
    ]

    /**
     * Create example of server response on /idx call
     * @param idx
     * @return
     */
    public Object getDetails(String idx) {
        int random = new Random().nextInt(sampleDescriptionsMap.size())
        String key = sampleDescriptionsMap.keySet()[random]
        Object value = sampleDescriptionsMap.get(key)
        def details = [
                id : idx,
                node: key,
                type: value
        ]
        details
    }
}

public enum TerminologyType {
    all, hosted, master, meta
}
