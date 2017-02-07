/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.ontology

import spock.lang.Specification

class DefaultExternalOntologyTermServiceSpec extends Specification {

    DefaultExternalOntologyTermService ontologyService
    String ontologyServerUrl
    String label
    String categoryCode
    List<String> list1
    List<String> list2
    List<String> list3

    def setup() {
        ontologyServerUrl = "http://testAddress"
        ontologyService = new DefaultExternalOntologyTermService(ontologyServerUrl)

        list1 = [
                "ROX32425774572250970",
                "ROX1305277804322",
                "ROX1305277804321",
                "ROX1394546352646",
                "ROX1312809955521"
        ]
        list2 = [
                "ROX32425774572250970",
                "ROX1305277804322",
                "ROX1305277804321",
                "ROX1394550342848",
                "ROX1305277804386",
                "ROX1312809955521"
        ]
        list3 = [
                "ROX32425774572250970",
                "ROX1305277804322",
                "ROX1305277804385",
                "ROX1305277804386",
                "ROX1312809955521"
        ]
        label = "Clear Cell Lung Carcinoma [Preflabel,Altlabel in Indication]"
        categoryCode = "testCategory"
    }

    void "test tree building"() {
        when:
        def map = ontologyService.createAncestorsMap([list1, list2, list3])

        then:
        // check number of nodes
        assert map.size() == (list1+list2+list3).unique { a, b -> a <=> b }.size()
        // check number of ancestors for each node
        assert map["ROX32425774572250970"].size() == 0
        assert map["ROX1312809955521"].size() == 2
        assert map["ROX1305277804386"].size() == 2
        assert map["ROX1305277804322"].size() == 1
        assert map["ROX1305277804321"].size() == 1
        assert map["ROX1394546352646"].size() == 1
        assert map["ROX1394550342848"].size() == 1
        assert map["ROX1305277804385"].size() == 1
    }

    void "test OntologyMap creation"() {
        // Test for root element
        def ancestorMap = [:]
        when:
        def ontologyCode = list1.first()
        ancestorMap.put(ontologyCode, null)
        OntologyMap ontologyMap = ancestorMap.collect { code, ancestors ->
            ontologyService.mapResponseToOntologyMap(
                code as String,
                label,
                categoryCode,
                ontologyCode,
                ancestors as List<String>
            )
        }.first()

        then:
        assert ontologyMap.categoryCode == categoryCode
        assert ontologyMap.dataLabel == ontologyCode
        assert ontologyMap.ontologyCode == ontologyCode
        assert ontologyMap.label == label
        assert ontologyMap.uri == "$ontologyServerUrl/$ontologyCode" as String
        assert ontologyMap.ancestors.every{it == null}

        // Test for last element
        when:
        ancestorMap = [:]
        def label2 = 'testLabel'
        ontologyCode = list1.last()
        ancestorMap.put(ontologyCode, [list1[-2], list2[-2]])

        ontologyMap = ancestorMap.collect { code, ancestors ->
            ontologyService.mapResponseToOntologyMap(
                    code as String,
                    label2,
                    null,
                    null,
                    ancestors as List<String>
            )
        }.first()

        then:
        assert ontologyMap.categoryCode == null
        assert ontologyMap.dataLabel == null
        assert ontologyMap.ontologyCode == ontologyCode
        assert ontologyMap.label == label2
        assert ontologyMap.uri == "$ontologyServerUrl/$ontologyCode" as String
        assert ontologyMap.ancestors == ancestorMap[ontologyCode]
    }
}



