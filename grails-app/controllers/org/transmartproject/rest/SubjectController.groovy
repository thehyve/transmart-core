package org.transmartproject.rest

import grails.converters.JSON
import grails.rest.Link
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.db.ontology.ConceptsResourceService
import org.transmartproject.rest.marshallers.CollectionResponseWrapper
import org.transmartproject.rest.marshallers.OntologyTermSerializationHelper
import org.transmartproject.rest.ontology.OntologyTermCategory

class SubjectController {

    static responseFormats = ['json', 'hal']

    StudyLoadingService studyLoadingServiceProxy
    PatientsResource    patientsResourceService
    ConceptsResourceService conceptsResourceService

    /** GET request on /studies/XXX/subjects/
     *  This will return the list of subjects for certain study,
     *  where each subject will be rendered in its short format
    */
    def index() {
        respond wrapSubjects(studyLoadingServiceProxy.study.patients, selfLinkForStudy())
    }

    /** GET request on /studies/XXX/subjects/${id}
     *  This returns the single subject for certain study.
     *
     *  @param id The is for which to return Data information.
     */
    def show(Integer id) {
        def patient = patientsResourceService.getPatientById(id)

        def studyName = studyLoadingServiceProxy.study.name
        if (patient.trial != studyName) {
            throw new NoSuchResourceException("The patient with id $id " +
                    "does not belong to the study '$studyName'")
        }

        respond patient
    }

    /** GET request on /studies/XXX/concepts/YYY/subjects
     *
     * @return list of subjects for study XXX and Data YYY
     */
    def indexByConcept() {
        use (OntologyTermCategory) {
            def ontologyTermKey = params.conceptId.keyFromURLPart(
                    studyLoadingServiceProxy.study)
            def ontologyTerm = conceptsResourceService.getByKey(ontologyTermKey)
            def patients = ontologyTerm.patients
            def selfLink = selfLinkForConcept ontologyTerm

            respond wrapSubjects(patients, selfLink)
        }
    }

    private def selfLinkForStudy() {
        "/studies/${studyLoadingServiceProxy.studyLowercase}/subjects"
    }

    private def selfLinkForConcept(OntologyTerm term) {
        use (OntologyTermCategory) {
            "/studies/${studyLoadingServiceProxy.studyLowercase}/concepts/" +
                    term.encodeAsURLPart(studyLoadingServiceProxy.study) +
            '/subjects'
        }
    }

    private def wrapSubjects(Object source, String selfLink) {

        new CollectionResponseWrapper(
                collection: source,
                componentType: Patient,
                links: [
                        new Link(grails.rest.render.util.AbstractLinkingRenderer.RELATIONSHIP_SELF,
                                selfLink
                        )
                ]
        )
    }

}
