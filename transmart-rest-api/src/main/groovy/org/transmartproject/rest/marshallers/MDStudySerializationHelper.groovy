package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription

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
        Study study = object.study
        def result = [
                id: study.id,
                studyId: study.studyId,
                bioExperimentId: study.bioExperimentId,
                dimensions: study.dimensionDescriptions.collect { DimensionDescription dim ->
                    dim.name
                }
        ]
        def conceptToVariableName = study.metadata?.conceptToVariableName
        if (conceptToVariableName) {
            result.metadata = [conceptToVariableName: conceptToVariableName]
        }
        result
    }

}
