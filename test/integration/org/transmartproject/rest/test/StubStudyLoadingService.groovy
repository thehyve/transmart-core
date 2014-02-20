package org.transmartproject.rest.test

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.rest.StudyLoadingService

class StubStudyLoadingService extends StudyLoadingService {

    Study storedStudy

    @Override
    Study fetchStudy() {
        storedStudy
    }

    static Study createStudy(String studyName, String key) {
        [
                getName: { -> studyName },
                getOntologyTerm: { ->
                    [
                            getName:     { -> getComponents(key, -1) },
                            getFullName: { -> '\\' + getComponents(key, 3, -1) + '\\' },
                            getKey: { -> key }
                    ] as OntologyTerm
                }
        ] as Study
    }

    private static String getComponents(String key, int a, int b = a) {
        (key.split('\\\\') as List)[a..b].join('\\')
    }
}
