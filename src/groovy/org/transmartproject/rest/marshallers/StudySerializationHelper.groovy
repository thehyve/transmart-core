package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.ontology.Study

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class StudySerializationHelper implements HalOrJsonSerializationHelper<Study> {

    final Class targetType = Study

    final String collectionName = 'studies'

    @Override
    Collection<Link> getLinks(Study study) {
        [new Link(RELATIONSHIP_SELF, '/studies/' +
                study.name.toLowerCase(Locale.ENGLISH).encodeAsURL())]
    }

    @Override
    Map<String, Object> convertToMap(Study study) {
        def term = new OntologyTermWrapper(study.ontologyTerm)
            [name: study.name,
                ontologyTerm: term]
    }

    @Override
    Set<String> getEmbeddedEntities(Study object) {
        ['ontologyTerm'] as Set
    }
}
