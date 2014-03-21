package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.rest.StudyLoadingService
import org.transmartproject.rest.ontology.OntologyTermCategory

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class OntologyTermSerializationHelper implements HalOrJsonSerializationHelper<OntologyTermWrapper> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    @Autowired
    StudiesResource studiesResourceService

    final Class targetType = OntologyTermWrapper

    final String collectionName = 'ontology_terms'

    @Override
    Collection<Link> getLinks(OntologyTermWrapper obj) {
        /* this gets tricky. We may be rendering this as part of the /studies response */
        OntologyTerm term = obj.delegate

        String studyName
        def pathPart

        try {
            studyName = studyLoadingServiceProxy.study.name
            use (OntologyTermCategory) {
                pathPart = term.encodeAsURLPart studyLoadingServiceProxy.study
            }
        } catch (InvalidArgumentsException iae) {
            /* studyId not in params; so we are handling a study, which
             * is mapped to $id (can we rename the param to $studyId
             * for consistency?)
             */
            studyName = term.name
            pathPart = 'ROOT'
        }
        studyName = studyName.toLowerCase(Locale.ENGLISH).encodeAsURL()

        // TODO add other relationships (children, parent, ...)
        [new Link(RELATIONSHIP_SELF, "/studies/$studyName/concepts/$pathPart")]
    }

    @Override
    Map<String, Object> convertToMap(OntologyTermWrapper obj) {
        OntologyTerm term = obj.delegate
        [
                name:     term.name,
                key:      term.key,
                fullName: term.fullName,
        ]
    }

    @Override
    Set<String> getEmbeddedEntities(OntologyTermWrapper object) {
        [] as Set
    }

}
