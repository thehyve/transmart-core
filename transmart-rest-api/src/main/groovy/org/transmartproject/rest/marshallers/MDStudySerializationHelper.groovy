package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.ontology.MDStudy

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF

class MDStudySerializationHelper extends AbstractHalOrJsonSerializationHelper<StudyWrapper> {

    final Class targetType = StudyWrapper

    final String collectionName = 'studies'

    @Override
    Collection<Link> getLinks(StudyWrapper study) {
        [new Link(RELATIONSHIP_SELF, "/${study.apiVersion}/studies/${study.study.id}")]
    }

    @Override
    Map<String, Object> convertToMap(StudyWrapper object) {
        MDStudy study = object.study
        def result = [
                id: study.id,
                studyId: study.name,
                bioExperimentId: study.bioExperimentId,
                dimensions: study.dimensions.collect { Dimension dim ->
                    dim.name
                }
        ]
        def studyMetadata = study.metadata
        if (studyMetadata) {
            result.metadata = studyMetadata
        }
        result
    }

}
