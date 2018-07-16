package org.transmartproject.rest.data

import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.type.StandardBasicTypes
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.multidimquery.TrialVisit
import org.transmartproject.db.i2b2data.*
import org.transmartproject.db.metadata.DimensionDescription

import java.time.Instant

@Slf4j
class AccessPolicyTestData extends org.transmartproject.rest.data.TestData {

    @Autowired
    SessionFactory sessionFactory

    void createTestData() {
        log.info "Setup access policy test data."
        // Create studies
        def publicStudy = createTestStudy('publicStudy', true, null)
        def study1 = createTestStudy('study1', false, null)
        def study2 = createTestStudy('study2', false, null)

        // Create concepts
        def concept1 = createTestConcept('categorical_concept1')
        def concept2 = createTestConcept('numerical_concept2')

        // Create patients
        def patient1 = createTestPatient('Subject 1')
        def patient2 = createTestPatient('Subject 2')
        def patient3 = createTestPatient('Subject 3')
        def patient4 = createTestPatient('Subject from public study')

        // Create observations
        // public study: 1 subject
        // study 1: 2 subjects (1 shared with study 2)
        // study 2: 2 subjects (1 shared with study 1)
        // total: 4 subjects
        Date dummyDate = Date.from(Instant.parse('2001-02-03T13:18:54Z'))
        createTestCategoricalObservations(patient1, concept1, study1[0], [['@': 'value1'], ['@': 'value2']], dummyDate)
        createTestNumericalObservations(patient1, concept2, study1[0], [['@': 100], ['@': 200]], dummyDate)
        createTestCategoricalObservations(patient2, concept1, study1[0], [['@': 'value2'], ['@': 'value3']], dummyDate)
        createTestNumericalObservations(patient2, concept2, study2[0], [['@': 400]], dummyDate)
        createTestCategoricalObservations(patient3, concept1, study2[0], [['@': 'value4']], dummyDate)
        createTestCategoricalObservations(patient4, concept1, publicStudy[0], [['@': 'value1']], dummyDate)
    }

    protected static final Set<String> defaultDimensions =
            ['study', 'concept', 'patient', 'start time', 'end time', 'trial visit'] as Set<String>

    protected List<TrialVisit> createTestStudy(String studyId, boolean isPublic, List<String> trialVisitLabels) {
        log.info "Creating test study: ${studyId}"
        def study = new Study(
                studyId: studyId,
                secureObjectToken: isPublic ? Study.PUBLIC : studyId,
                trialVisits: [] as Set,
                dimensionDescriptions: [] as Set
        )
        study.save(flush: true, failOnError: true)
        // add trial visits
        if (trialVisitLabels) {
            for (String label : trialVisitLabels) {
                study.trialVisits.add(
                        new org.transmartproject.db.i2b2data.TrialVisit(study: study, relTimeLabel: label)
                                .save(flush: true, failOnError: true))
            }
        } else {
            study.trialVisits.add(
                    new org.transmartproject.db.i2b2data.TrialVisit(study: study, relTimeLabel: 'default')
                            .save(flush: true, failOnError: true))
        }
        // add dimension descriptions
        def dimensionDescriptions = DimensionDescription.createCriteria().list {
            'in'('name', defaultDimensions)
        } as List<DimensionDescription>
        study.dimensionDescriptions.addAll(dimensionDescriptions)
        study.save(flush: true, failOnError: true)
        study.trialVisits as List<TrialVisit>
    }

    protected Concept createTestConcept(String conceptCode) {
        log.info "Creating test concept: ${conceptCode}"
        new ConceptDimension(conceptCode: conceptCode, conceptPath: "\\Test\\${conceptCode}")
                .save(flush: true, failOnError: true)
    }

    protected long getNextHibernateId() {
        sessionFactory.currentSession.createSQLQuery(
                "select hibernate_sequence.nextval as num")
                .addScalar("num", StandardBasicTypes.BIG_INTEGER)
                .uniqueResult() as Long
    }

    protected Patient createTestPatient(String subjectId) {
        long patientId = nextHibernateId
        log.info "Creating test patient: ${subjectId} (id: ${patientId})"
        def patient = new PatientDimension(mappings: [] as Set)
        patient.id = patientId
        patient.save(flush: true, failOnError: true)
        def patientMapping = new PatientMapping(patient: patient, encryptedId: subjectId, source: 'SUBJ_ID')
        patientMapping.save(flush: true, failOnError: true)
        patient.mappings.add(patientMapping)
        patient.save(flush: true, failOnError: true)
    }

    protected void createTestCategoricalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String, String>> values, Date startDate) {
        log.info "Adding ${values?.size()} observations for patient: ${patient.subjectIds}, concept: ${concept.conceptCode}"
        int instanceNum = 1
        for (Map<String, String> valueMap : values) {
            for (def entry : valueMap.entrySet())
                new ObservationFact(
                        valueType: ObservationFact.TYPE_TEXT,
                        modifierCd: entry.key,
                        textValue: entry.value,
                        conceptCode: concept.conceptCode,
                        trialVisit: (org.transmartproject.db.i2b2data.TrialVisit) trialVisit,
                        patient: (PatientDimension) patient,
                        startDate: startDate,
                        providerId: '@',
                        encounterNum: 1,
                        instanceNum: instanceNum
                ).save(flush: true, failOnError: true)
            instanceNum++
        }
    }

    protected void createTestNumericalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String, BigDecimal>> values, Date startDate) {
        log.info "Adding ${values?.size()} observations for patient: ${patient.subjectIds}, concept: ${concept.conceptCode}"
        int instanceNum = 1
        for (Map<String, BigDecimal> valueMap : values) {
            for (def entry : valueMap.entrySet())
                new ObservationFact(
                        valueType: ObservationFact.TYPE_NUMBER,
                        modifierCd: entry.key,
                        numberValue: entry.value,
                        conceptCode: concept.conceptCode,
                        trialVisit: (org.transmartproject.db.i2b2data.TrialVisit) trialVisit,
                        patient: (PatientDimension) patient,
                        startDate: startDate,
                        providerId: '@',
                        encounterNum: 1,
                        instanceNum: instanceNum
                ).save(flush: true, failOnError: true)
            instanceNum++
        }
    }

}
