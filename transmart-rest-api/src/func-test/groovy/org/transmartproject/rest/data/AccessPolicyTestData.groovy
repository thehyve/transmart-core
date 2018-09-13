package org.transmartproject.rest.data

import grails.transaction.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Restrictions
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.multidimquery.TrialVisit
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.tree.TreeNode
import org.transmartproject.db.Dictionaries
import org.transmartproject.db.i2b2data.*
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.tree.TreeNodeImpl
import org.transmartproject.db.utils.SessionUtils

import java.time.Instant

import static java.util.Objects.requireNonNull
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.CATEGORICAL

@Slf4j
class AccessPolicyTestData extends org.transmartproject.rest.data.TestData {

    @Transactional
    void createTestData() {
        new Dictionaries().saveAll()

        log.info "Setup access policy test data."
        // Create studies
        List<TrialVisit> publicStudyTrialVisits = createTestStudyAndTrialVisits('publicStudy', true, null)
        def publicStudy = publicStudyTrialVisits[0].study
        def studiesNode = createTestTreeNode('', 'Studies', null, null, null)
        def publicStudyNode = createTestTreeNode(studiesNode.fullName, 'Public study', null, null, publicStudy)
        List<TrialVisit> study1TrialVisits = createTestStudyAndTrialVisits('study1', false, null)
        def study1 = study1TrialVisits[0].study
        List<TrialVisit> study2TrialVisits = createTestStudyAndTrialVisits('study2', false, null)
        def study2 = study2TrialVisits[0].study
        def study1Node = createTestTreeNode(studiesNode.fullName, 'Study 1', null, null, study1)

        // Create concepts
        def concept1 = createTestConcept('categorical_concept1')
        def concept2 = createTestConcept('numerical_concept2')

        def conceptsNode = createTestTreeNode('', 'Concepts', null, null, null)
        def concept1Node = createTestTreeNode(conceptsNode.fullName, 'Categorical concept 1', concept1.conceptPath, CATEGORICAL, null)
        def concept2Node = createTestTreeNode(conceptsNode.fullName, 'Categorical concept 2', concept2.conceptPath, CATEGORICAL, null)

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
        createTestCategoricalObservations(patient1, concept1, study1TrialVisits[0], [['@': 'value1'], ['@': 'value2']], dummyDate)
        createTestNumericalObservations(patient1, concept2, study1TrialVisits[0], [['@': 100], ['@': 200]], dummyDate)
        createTestCategoricalObservations(patient2, concept1, study1TrialVisits[0], [['@': 'value2'], ['@': 'value3']], dummyDate)

        createTestNumericalObservations(patient2, concept2, study2TrialVisits[0], [['@': 400]], dummyDate)
        createTestCategoricalObservations(patient3, concept1, study2TrialVisits[0], [['@': 'value4']], dummyDate)

        createTestCategoricalObservations(patient4, concept1, publicStudyTrialVisits[0], [['@': 'value1']], dummyDate)
    }

    protected static final Set<String> defaultDimensions =
            ['study', 'concept', 'patient', 'start time', 'end time', 'trial visit'] as Set<String>

    protected List<TrialVisit> createTestStudyAndTrialVisits(String studyId, boolean isPublic, List<String> trialVisitLabels) {
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

    protected TreeNode createTestTreeNode(
            String parentPath, String name, String conceptPath, OntologyTerm.VisualAttributes conceptType, MDStudy study) {
        def path = "${parentPath}\\${name}\\"
        log.info "Creating test tree node: ${path}"
        def parent = DetachedCriteria.forClass(I2b2Secure)
                .add(Restrictions.eq('fullName', parentPath))
                .getExecutableCriteria(sessionFactory.currentSession).uniqueResult() as I2b2Secure
        if (parentPath && !parent) {
            throw new IllegalArgumentException("Parent node not found: ${parentPath}")
        }

        def visualAttributes = 'FA '
        def factTableColumn = ''
        def dimensionTableName = ''
        def columnName = ''
        def operator = ''
        def dimensionCode = ''

        if (conceptPath) {
            requireNonNull(conceptType)
            assert conceptType.position == 2: "Invalid concept type"
            visualAttributes = "LA${conceptType.keyChar}"
            factTableColumn = 'concept_cd'
            dimensionTableName = 'concept_dimension'
            columnName = 'concept_path'
            operator = 'like'
            dimensionCode = conceptPath
        } else if (study) {
            visualAttributes = 'FAS'
            factTableColumn = ''
            dimensionTableName = 'study'
            columnName = 'study_id'
            operator = '='
            dimensionCode = study.name
        }

        def node = new I2b2Secure(
                secureObjectToken: study?.secureObjectToken ?: Study.PUBLIC,
                level: (parent?.level ?: 0) + 1,
                fullName: path,
                name: name,
                visualAttributes: visualAttributes,
                factTableColumn: factTableColumn,
                dimensionTableName: dimensionTableName,
                columnName: columnName,
                columnDataType: 'T',
                operator: operator,
                dimensionCode: dimensionCode
        ).save(flush: true, failOnError: true)
        new TreeNodeImpl(node, [])
    }


    protected Patient createTestPatient(String subjectId) {
        long patientId = SessionUtils.getNextId(sessionFactory.currentSession, 'hibernate_sequence')
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
                        encounterNum: -1,
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
                        encounterNum: -1,
                        instanceNum: instanceNum
                ).save(flush: true, failOnError: true)
            instanceNum++
        }
    }

}
