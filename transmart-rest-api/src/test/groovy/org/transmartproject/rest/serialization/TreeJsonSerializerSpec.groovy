package org.transmartproject.rest.serialization

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.serialization.TreeJsonSerializer
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.tree.TreeNodeImpl
import spock.lang.Specification

class TreeJsonSerializerSpec extends Specification {

    void 'test tree node constraint serialisation'() {
        given:
        def studyNode = Mock(I2b2Secure, {
            getFullName() >> '\\Studies\\Survey 0\\'
            getName() >> 'Survey 0'
            getVisualAttributes() >> OntologyTerm.VisualAttributes.forSequence('FAS')
            getDimensionTableName() >> 'study'
            getColumnName() >> 'study_id'
            getOperator() >> '='
            getDimensionCode() >> 'SURVEY0'
        })
        def i2b2Node = Mock(I2b2Secure, {
            getFullName() >> '\\Studies\\Survey 0\\Birth date\\'
            getName() >> 'Birth date'
            getVisualAttributes() >> OntologyTerm.VisualAttributes.forSequence('LAN')
            getDimensionTableName() >> 'concept_dimension'
            getColumnName() >> 'concept_cd'
            getOperator() >> '='
            getDimensionCode() >> 'birthdate'
        })
        def node = new TreeNodeImpl(i2b2Node, [])
        def parentNode = new TreeNodeImpl(studyNode, [node])
        node.conceptCode = 'birthdate'
        node.parent = parentNode

        when:
        def out = new ByteArrayOutputStream()
        new TreeJsonSerializer().write([writeConstraints: true, writeTags: false], node, out)

        then:
        out.toString() == '{"name":"Birth date","fullName":"\\\\Studies\\\\Survey 0\\\\Birth date\\\\","studyId":"SURVEY0","conceptCode":"birthdate","type":"NUMERIC","visualAttributes":["LEAF","ACTIVE","NUMERICAL"],"constraint":{"type":"and","args":[{"type":"concept","conceptCode":"birthdate"},{"type":"study_name","studyId":"SURVEY0"}]}}'
    }

}
