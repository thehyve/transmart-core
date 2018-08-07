package org.transmartproject.rest.v2

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.transmartproject.core.multidimquery.Counts
import org.transmartproject.core.multidimquery.CrossTable
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.mock.MockUser
import spock.lang.Shared
import spock.lang.Unroll

import java.time.Instant

import static org.springframework.http.HttpStatus.FORBIDDEN
import static org.springframework.http.HttpStatus.OK
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.CATEGORICAL
import static org.transmartproject.core.users.PatientDataAccessLevel.*
import static org.transmartproject.rest.utils.ResponseEntityUtils.checkResponseStatus
import static org.transmartproject.rest.utils.ResponseEntityUtils.toObject

@Slf4j
class AccessPolicySpec extends V2ResourceSpec {

    @CompileStatic
    @Canonical
    class ConstraintRequestBody {
        Constraint constraint
    }

    @Shared
    MockUser admin = new MockUser('admin', true)
    @Shared
    MockUser study1User = new MockUser('study1User', [study1: MEASUREMENTS])
    @Shared
    MockUser thresholdUser = new MockUser('thresholdUser', [study1: COUNTS_WITH_THRESHOLD])
    @Shared
    MockUser study2User = new MockUser('study2User', [study2: SUMMARY])
    @Shared
    MockUser study1And2User = new MockUser('study1And2User', [study1: SUMMARY, study2: SUMMARY])

    @Shared
    Constraint trueConstraint = new TrueConstraint()
    @Shared
    Constraint study1Constraint = new StudyNameConstraint('study1')
    @Shared
    Constraint concept1Value1 = new AndConstraint([
            new ConceptConstraint('categorical_concept1'),
            new ValueConstraint(Type.STRING, Operator.EQUALS, 'value1')])
    @Shared
    Constraint concept1Value2 = new AndConstraint([
            new ConceptConstraint('categorical_concept1'),
            new ValueConstraint(Type.STRING, Operator.EQUALS, 'value2')])


    void setupData() {
        testResource.clearTestData()

        // Create studies
        def publicStudyTrialVisits = testResource.createTestStudy('publicStudy', true, null)
        def publicStudy = publicStudyTrialVisits[0].study
        def studiesNode = testResource.createTestTreeNode('', 'Studies', null, null, null)
        def publicStudyNode = testResource.createTestTreeNode(studiesNode.fullName, 'Public study', null, null, publicStudy)
        def study1TrialVisits = testResource.createTestStudy('study1', false, null)
        def study1 = study1TrialVisits[0].study
        def study2TrialVisits = testResource.createTestStudy('study2', false, null)
        def study1Node = testResource.createTestTreeNode(studiesNode.fullName, 'Study 1', null, null, study1)

        // Create concepts
        def concept1 = testResource.createTestConcept('categorical_concept1')
        def concept2 = testResource.createTestConcept('numerical_concept2')

        def conceptsNode = testResource.createTestTreeNode('', 'Concepts', null, null, null)
        def concept1Node = testResource.createTestTreeNode(conceptsNode.fullName, 'Categorical concept 1', concept1.conceptPath, CATEGORICAL, null)
        def concept2Node = testResource.createTestTreeNode(conceptsNode.fullName, 'Categorical concept 2', concept2.conceptPath, CATEGORICAL, null)

        // Create patients
        def patient1 = testResource.createTestPatient('Subject 1')
        def patient2 = testResource.createTestPatient('Subject 2')
        def patient3 = testResource.createTestPatient('Subject 3')
        def patient4 = testResource.createTestPatient('Subject from public study')

        // Create observations
        // public study: 1 subject
        // study 1: 2 subjects (1 shared with study 2)
        // study 2: 2 subjects (1 shared with study 1)
        // total: 4 subjects
        Date dummyDate = Date.from(Instant.parse('2001-02-03T13:18:54Z'))
        testResource.createTestCategoricalObservations(patient1, concept1, study1TrialVisits[0], [['@': 'value1'], ['@': 'value2']], dummyDate)
        testResource.createTestNumericalObservations(patient1, concept2, study1TrialVisits[0], [['@': 100], ['@': 200]], dummyDate)
        testResource.createTestCategoricalObservations(patient2, concept1, study1TrialVisits[0], [['@': 'value2'], ['@': 'value3']], dummyDate)
        testResource.createTestNumericalObservations(patient2, concept2, study1TrialVisits[0], [['@': 300]], dummyDate)
        testResource.createTestNumericalObservations(patient2, concept2, study2TrialVisits[0], [['@': 400]], dummyDate)
        testResource.createTestCategoricalObservations(patient3, concept1, study2TrialVisits[0], [['@': 'value4']], dummyDate)
        testResource.createTestCategoricalObservations(patient4, concept1, publicStudyTrialVisits[0], [['@': 'value1']], dummyDate)
    }

    /**
     * Tests the /observations/counts endpoint at {@link org.transmartproject.rest.QueryController#counts()}.
     */
    @Unroll
    void 'test access to counts'(
            Constraint constraint, MockUser user, HttpStatus expectedStatus, Long patientCount) {
        given:
        setupData()
        def url = "${contextPath}/observations/counts"

        expect:
        selectUser(user)
        def body = new ConstraintRequestBody(constraint)

        def response = post(url, body)
        checkResponseStatus(response, expectedStatus, user)
        switch (expectedStatus) {
            case OK:
                def counts = toObject(response, Counts)
                assert counts
                assert counts.patientCount == patientCount:
                        "Unexpected patient count ${counts.patientCount} for user ${user.username}, " +
                                "constraint ${constraint.toJson()}. Expected ${patientCount}."
                break
            case FORBIDDEN:
                def error = toObject(response, Map)
                assert (error.message as String).startsWith('Access denied to study or study does not exist')
                break
            default:
                throw new IllegalArgumentException("Unexpected status: ${expectedStatus.value()}")
        }

        where:
        constraint       | user           | expectedStatus | patientCount
        trueConstraint   | admin          | OK             | 4
        trueConstraint   | study1User     | OK             | 3
        trueConstraint   | thresholdUser  | OK             | 1
        trueConstraint   | study2User     | OK             | 3
        trueConstraint   | study1And2User | OK             | 4
        study1Constraint | admin          | OK             | 2
        study1Constraint | study1User     | OK             | 2
        study1Constraint | thresholdUser  | FORBIDDEN      | null
        study1Constraint | study2User     | FORBIDDEN      | null
        study1Constraint | study1And2User | OK             | 2
    }

    @CompileStatic
    @Canonical
    class CrossTableRequestBody {
        List<Constraint> rowConstraints
        List<Constraint> columnConstraints
        Constraint subjectConstraint
    }

    /**
     * Tests the /observations/crosstable endpoint at {@link org.transmartproject.rest.QueryController#crosstable()}
     */
    @Unroll
    void 'test access to the cross table'(
            List<Constraint> rowConstraints, List<Constraint> columnConstraints, Constraint subjectConstraint,
            MockUser user, HttpStatus expectedStatus, List<List<Long>> expectedValues) {
        given:
        setupData()
        def url = "${contextPath}/observations/crosstable"

        expect:
        selectUser(user)
        def body = new CrossTableRequestBody(rowConstraints, columnConstraints, subjectConstraint)

        log.info "Body: ${new ObjectMapper().writeValueAsString(body)}"
        def response = post(url, body)
        checkResponseStatus(response, expectedStatus, user)
        switch (expectedStatus) {
            case OK:
                def table = toObject(response, CrossTable)
                log.info "Cross table: ${new ObjectMapper().writeValueAsString(table)}"
                assert table
                assert table.rows == expectedValues
                break
            case FORBIDDEN:
                def error = toObject(response, Map)
                assert (error.message as String).startsWith('Access denied to study or study does not exist')
                break
            default:
                throw new IllegalArgumentException("Unexpected status: ${expectedStatus.value()}")
        }

        where:
        rowConstraints   | columnConstraints | subjectConstraint | user           | expectedStatus | expectedValues
        [concept1Value1] | [concept1Value2]  | trueConstraint    | admin          | OK             | [[1]]
        [concept1Value1] | [concept1Value2]  | trueConstraint    | study1User     | OK             | [[1]]
        [concept1Value1] | [concept1Value2]  | trueConstraint    | thresholdUser  | OK             | [[0]]
        [concept1Value1] | [concept1Value2]  | trueConstraint    | study2User     | OK             | [[0]]
        [concept1Value1] | [concept1Value2]  | trueConstraint    | study1And2User | OK             | [[1]]
    }

    @Unroll
    void 'test dimension elements access (POST .../dimensions/#dimension/elements) for #user.username.'(
            MockUser user, String dimension, HttpStatus expectedStatus
    ) {
        given:
        setupData()
        def url = "${contextPath}/dimensions/${dimension}/elements"
        def constraint = study1Constraint

        when:
        selectUser(user)
        def body = new ConstraintRequestBody(constraint)
        def response = post(url, body)

        then:
        checkResponseStatus(response, expectedStatus, user)

        where:
        user           | dimension     | expectedStatus
        study1User     | 'patient'     | OK
        study1User     | 'concept'     | OK
        study1User     | 'study'       | OK
        study1User     | 'trial visit' | OK
        thresholdUser  | 'patient'     | FORBIDDEN
        thresholdUser  | 'concept'     | OK
        thresholdUser  | 'study'       | OK
        thresholdUser  | 'trial visit' | OK
        study1And2User | 'patient'     | FORBIDDEN
    }

}
