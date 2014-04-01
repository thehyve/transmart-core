package org.transmartproject.rest.marshallers

import grails.rest.Link
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.rest.StudyLoadingService

import javax.annotation.Resource

import static grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF
import static org.transmartproject.rest.marshallers.MarshallerSupport.getPropertySubsetForSuperType

class PatientSerializationHelper implements HalOrJsonSerializationHelper<Patient> {

    @Resource
    StudyLoadingService studyLoadingServiceProxy

    final Class targetType = Patient

    final String collectionName = 'subjects'

    def convert(Patient patient) {
        getPropertySubsetForSuperType(patient, Patient, ['assays'] as Set)
    }

    @Override
    Collection<Link> getLinks(Patient patient) {
        def studyName = studyLoadingServiceProxy.studyLowercase.encodeAsURL()

        //TODO add more relationships (for instance, the parent study)
        [new Link(RELATIONSHIP_SELF, "/studies/$studyName/subjects/$patient.id")]
    }

    @Override
    Map<String, Object> convertToMap(Patient patient) {
        Map<String, Object> result = getPropertySubsetForSuperType(patient, Patient, ['assays', 'sex'] as Set)
        result.put('sex', patient.sex.name()) //sex has to be manually converted (no support for enums)
        result
    }

    @Override
    Set<String> getEmbeddedEntities(Patient patient) {
        [] as Set
    }
}
