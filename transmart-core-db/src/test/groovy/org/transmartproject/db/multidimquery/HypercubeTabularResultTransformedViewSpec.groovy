package org.transmartproject.db.multidimquery

import spock.lang.Specification

class HypercubeTabularResultTransformedViewSpec extends Specification {

    def hypercubeTabularResultView = Mock(HypercubeTabularResultView)
    def testee = new HypercubeTabularResultTransformedView(hypercubeTabularResultView)
    def column = Mock(HypercubeDataColumn)

    def setup() {
        column.key >> [-1]
        hypercubeTabularResultView.indicesList >> [ column ]
    }

    def 'merge NA modifier column with the data column'() {
        testee.indicesList
    }

}
