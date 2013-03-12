package org.transmartproject.db.support

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes
import org.transmartproject.db.marshallers.OntologyTermMarshaller

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import org.junit.*

class MarshallerRegistrarServiceTests {

    def grailsApplication

    static class ExtendedOntologyTerm implements OntologyTerm {
        final Integer level = 1
        final String key = '\\\\foo_bar\\my full name\\'
        final String fullName = "\\my full name\\"
        final String name = "my name"
        final String tooltip = tooltip
        final EnumSet<VisualAttributes> visualAttributes = EnumSet.allOf(VisualAttributes)
        @Override
        List<OntologyTerm> getChildren(boolean showHidden = false,
                                       boolean showSynonyms = false) {
            []
        }
        final String extra = 'extra'
    }

    @Test
    void testRegistersOntologyTermMarshaller() {
        // The registrar is in the application context
        assertThat grailsApplication.mainContext.
                getBean(MarshallerRegistrarService), is(notNullValue())

        // The registrar has registered the OntologyTermMarshaller
        assertThat grailsApplication.mainContext.
                getBean(OntologyTermMarshaller), is(notNullValue())

        def term = new ExtendedOntologyTerm()

        // The marshaller is being used
        def termOut = new JsonSlurper().parseText((term as JSON).toString())
        assertThat termOut, allOf(
                hasEntry('fullName', '\\my full name\\'),
                not(hasEntry('extra', 'extra')))
    }

}
