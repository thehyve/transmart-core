package mock.ontology.server

import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Created by ewelina on 8-12-16.
 */
class OntologyTermResponseGenerator {

    public static final Boolean USE_EXTERNAL_SERVER_RESPONSE = false
    public static final String mainDefaultCode = "ROX1381416546756"
    public static final  String mainSnomedCode = "SNOMEDCT364075005"

    public static final List<List<String>> defaultClasspath = [
            [
                    "ROX32425774572250970",
                    "ROX1305277804322",
                    "ROX1305277804321",
                    "ROX1366314046215",
                    "ROX1381416546756"
            ],
            [
                    "ROX32425774572250970",
                    "ROX1305277804322",
                    "ROX1305277804321",
                    "ROX1394550342848",
                    "ROX1305277804386",
                    "ROX1381416546756"
            ],
            [
                    "ROX32425774572250970",
                    "ROX1305277804322",
                    "ROX1305277804385",
                    "ROX1305277804386",
                    "ROX1381416546756"
            ]
    ]

    public static final List<List<String>> snomedClasspath = [
            [
                    "SNOMEDCT363787002",
                    "SNOMEDCT46680005",
                    "SNOMEDCT78564009",
                    "SNOMEDCT364075005"
            ],
            [
                    "SNOMEDCT363787002",
                    "SNOMEDCT414237002",
                    "SNOMEDCT414236006",
                    "SNOMEDCT364072008",
                    "SNOMEDCT364075005"
            ],

            [
                    "SNOMEDCT363787002",
                    "SNOMEDCT363788007",
                    "SNOMEDCT364066008",
                    "SNOMEDCT364072008",
                    "SNOMEDCT364075005"
            ],
            [
                    "SNOMEDCT363787002",
                    "SNOMEDCT363788007",
                    "SNOMEDCT364066008",
                    "SNOMEDCT310611001",
                    "SNOMEDCT78564009",
                    "SNOMEDCT364075005"
            ],
            [
                    "SNOMEDCT363787002",
                    "SNOMEDCT363788007",
                    "SNOMEDCT364066008",
                    "SNOMEDCT364087003",
                    "SNOMEDCT364088008",
                    "SNOMEDCT364089000",
                    "SNOMEDCT364093006",
                    "SNOMEDCT248627000",
                    "SNOMEDCT78564009",
                    "SNOMEDCT364075005"
            ]
    ]

    public static final Map<String, String> labels = [
            "ROX32425774572250970" : "Indication",
            "ROX1305277804322"     : "Cancer",
            "ROX1305277804321"     : "Carcinoma",
            "ROX1366314046215"     : "Adenosquamous Carcinoma",
            "ROX1394550342848"     : "Ductal Carcinoma In Situ",
            "ROX1305277804386"     : "Invasive Ductal Carcinoma",
            "ROX1305277804385"     : "Invasive Lobular Carcinoma",
            "ROX1381416546756"     : "Adenosquamous Cell Lung Carcinoma",
            "SNOMEDCT363787002"    : "Observable entity",
            "SNOMEDCT46680005"     : "Vital signs",
            "SNOMEDCT363788007"    : "Clinical history/examination observable",
            "SNOMEDCT414237002"    : "Feature of entity",
            "SNOMEDCT414236006"    : "Feature of anatomical entity",
            "SNOMEDCT364066008"    : "Cardiac feature",
            "SNOMEDCT364072008"    : "Cardiac feature",
            "SNOMEDCT364087003"    : "Blood vessel feature",
            "SNOMEDCT364088008"    : "Arterial feature",
            "SNOMEDCT364089000"    : "Systemic arterial feature",
            "SNOMEDCT364093006"    : "Feature of peripheral pulse",
            "SNOMEDCT248627000"    : "Pulse characteristics",
            "SNOMEDCT310611001"    : "Cardiovascular measure",
            "SNOMEDCT78564009"     : "Pulse rate",
            "SNOMEDCT364075005"    : "Heart rate"
    ]

    public static final Map<String, String> sampleDescriptionsMap = [
            "Indication"                       : "master terminology",
            "Observable entity"                : "master terminology",
            "Cancer"                           : "top concept of master terminology",
            "Vital signs"                      : "top concept of master terminology",
            "Carcinoma"                        : "narrowed concept of 'Cancer'",
            "Adenosquamous Cell Lung Carcinoma": "narrower concept and search hit",
            "Heart rate"                       : "narrower concept and search hit",
            "Default label"                    : "narrowed concept"
    ]

    /**
     * Create example of server response on /search/conceptCode call
     * @param idx
     * @return
     */
    public Object fetchPreferredConcepts(String conceptCode) {
        if (USE_EXTERNAL_SERVER_RESPONSE) {
            return getResponseFromExternalServer(conceptCode)
        } else if (conceptCode == mainSnomedCode){
            def elem = [
                    getResponseElement("150", labels[mainSnomedCode], TerminologyType.hosted, snomedClasspath),
                    getResponseElement("20", labels[mainDefaultCode], TerminologyType.master, defaultClasspath),
            ]
            return elem
        } else {
            def elem = [
                    getResponseElement("130", labels[mainDefaultCode], TerminologyType.master, defaultClasspath),
                    getResponseElement("16.5", labels[mainSnomedCode], TerminologyType.hosted, snomedClasspath)
            ]
            return elem
        }
    }

    /**
     * Redirect fetching ontology term from external web server
     * @param conceptCode
     * @return
     */
    private List<String> getResponseFromExternalServer(String conceptCode) {
        throw new NotImplementedException()
        //TODO: use for example http://sparql.bioontology.org/examples to get more realistic data
    }

    /**
     * Create terminology search endpoint response: /search/id
     * @param score
     * @param label
     * @param terminologyType
     * @param classpath
     * @return
     */
    private static Object getResponseElement(String score, String label = "narrowed concept", TerminologyType terminologyType,
                                             List classpath) {
        def element = [
                score           : score,
                label           : label,
                terminology_type: terminologyType.name(),
                classpath       : classpath
        ]
        element
    }

    /**
     * Create terminology identifier endpoint response: /idx
     * @param idx
     * @return
     */
    public Object getDetails(String idx) {
        String node = labels[idx] == null ? "Default label" : labels[idx]
        Object value = sampleDescriptionsMap[node]
        def details = [
                id : idx,
                node: node,
                type: value
        ]
        details
    }
}

public enum TerminologyType {
    all, hosted, master, meta
}
