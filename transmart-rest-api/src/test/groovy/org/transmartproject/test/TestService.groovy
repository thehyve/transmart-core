package org.transmartproject.test

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.type.StandardBasicTypes
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.multidimquery.TrialVisit
import org.transmartproject.db.Dictionaries
import org.transmartproject.db.TestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.PatientMapping
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.querytool.QtQueryResultType
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.rest.TestResource

@Slf4j
@CompileStatic
@Transactional
class TestService implements TestResource {

    @Autowired
    SessionFactory sessionFactory

    final Dictionaries dictionaries = new Dictionaries()

    void clearTestData() {
        log.info "Clear test data ..."
        TestData.clearAllData()
    }

    void createTestData() {
        clearTestData()

        log.info "Setup test data ..."
        def session = sessionFactory.currentSession
        // Check if dictionaries were already loaded before
        def resultTypes = DetachedCriteria.forClass(QtQueryResultType).getExecutableCriteria(session).list() as List<QtQueryResultType>
        if (resultTypes.size() == 0) {
            log.info "Setup test database"
            this.dictionaries.saveAll()
        }
        // Check if test data has been created before
        def nodes = DetachedCriteria.forClass(I2b2Secure).getExecutableCriteria(session).list() as List<I2b2Secure>
        if (nodes.size() == 0) {
            log.info "Create test data"
            def testData = TestData.createDefault()
            testData.saveAll()
            new org.transmartproject.rest.TestData().createTestData()
            def accessLevelTestData = AccessLevelTestData.createWithAlternativeConceptData(testData.conceptData)
            accessLevelTestData.saveAll()
        }
    }

    static final Set<String> defaultDimensions =
            ['study', 'concept', 'patient', 'start time', 'end time', 'trial visit'] as Set<String>

    List<TrialVisit> createTestStudy(String studyId, boolean isPublic, List<String> trialVisitLabels) {
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
            for (String label: trialVisitLabels) {
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

    Concept createTestConcept(String conceptCode) {
        log.info "Creating test concept: ${conceptCode}"
        new ConceptDimension(conceptCode: conceptCode, conceptPath: "\\Test\\${conceptCode}")
                .save(flush: true, failOnError: true)
    }

    private long getNextHibernateId() {
        sessionFactory.currentSession.createSQLQuery(
                "select hibernate_sequence.nextval as num")
                .addScalar("num", StandardBasicTypes.BIG_INTEGER)
                .uniqueResult() as Long
    }

    @Override
    Patient createTestPatient(String subjectId) {
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

    @Override
    void createTestCategoricalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String, String>> values, Date startDate) {
        log.info "Adding ${values?.size()} observations for patient: ${patient.subjectIds}, concept: ${concept.conceptCode}"
        int instanceNum = 1
        for (Map<String, String> valueMap: values) {
            for (def entry: valueMap.entrySet())
            new ObservationFact(
                    valueType: ObservationFact.TYPE_TEXT,
                    modifierCd: entry.key,
                    textValue: entry.value,
                    conceptCode: concept.conceptCode,
                    trialVisit: (org.transmartproject.db.i2b2data.TrialVisit)trialVisit,
                    patient: (PatientDimension)patient,
                    startDate: startDate,
                    providerId: '@',
                    encounterNum: 1,
                    instanceNum: instanceNum
            ).save(flush: true, failOnError: true)
            instanceNum++
        }
    }

    @Override
    void createTestNumericalObservations(
            Patient patient, Concept concept, TrialVisit trialVisit, List<Map<String, BigDecimal>> values, Date startDate) {
        log.info "Adding ${values?.size()} observations for patient: ${patient.subjectIds}, concept: ${concept.conceptCode}"
        int instanceNum = 1
        for (Map<String, BigDecimal> valueMap: values) {
            for (def entry: valueMap.entrySet())
                new ObservationFact(
                        valueType: ObservationFact.TYPE_NUMBER,
                        modifierCd: entry.key,
                        numberValue: entry.value,
                        conceptCode: concept.conceptCode,
                        trialVisit: (org.transmartproject.db.i2b2data.TrialVisit)trialVisit,
                        patient: (PatientDimension)patient,
                        startDate: startDate,
                        providerId: '@',
                        encounterNum: 1,
                        instanceNum: instanceNum
                ).save(flush: true, failOnError: true)
            instanceNum++
        }
    }

}
