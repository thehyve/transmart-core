package org.transmartproject.db.marshallers

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.junit.Test
import org.transmartproject.core.ontology.OntologyTerm

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class EnumMarshallerTests {

    @Test
    void testMarshalling() {
        def values = [
                OntologyTerm.VisualAttributes.HIDDEN,
                OntologyTerm.VisualAttributes.MODIFIER_CONTAINER,
        ]

        def out = new JsonSlurper().parseText((values as JSON).toString())
        assertThat out, contains(
                equalTo('HIDDEN'),
                equalTo('MODIFIER_CONTAINER')
        )
    }

}
