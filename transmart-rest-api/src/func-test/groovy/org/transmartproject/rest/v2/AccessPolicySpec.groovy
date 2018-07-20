package org.transmartproject.rest.v2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.transmartproject.core.config.SystemResource
import org.transmartproject.core.multidimquery.ConstraintHolder
import org.transmartproject.core.multidimquery.aggregates.Aggregates
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.counts.CountsPerConcept
import org.transmartproject.core.multidimquery.counts.CountsPerStudy
import org.transmartproject.core.multidimquery.counts.CountsPerStudyAndConcept
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.crosstable.CrossTableRequest
import org.transmartproject.core.multidimquery.datatable.DataTable
import org.transmartproject.core.multidimquery.datatable.DataTableRequest
import org.transmartproject.core.multidimquery.datatable.DimensionRequest
import org.transmartproject.core.multidimquery.export.ExportElement
import org.transmartproject.core.multidimquery.export.ExportJobRepresentation
import org.transmartproject.core.multidimquery.export.Format
import org.transmartproject.core.multidimquery.hypercube.Hypercube
import org.transmartproject.core.multidimquery.hypercube.HypercubeRequest
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.db.clinical.CountsWithThresholdService
import org.transmartproject.mock.MockUser
import org.transmartproject.rest.data.AccessPolicyTestData
import spock.lang.Shared
import spock.lang.Unroll

import static org.springframework.http.HttpStatus.*
import static org.transmartproject.core.users.PatientDataAccessLevel.*
import static org.transmartproject.rest.utils.ResponseEntityUtils.checkResponseStatus
import static org.transmartproject.rest.utils.ResponseEntityUtils.toObject

class AccessPolicySpec extends V2ResourceSpec {

    @Shared
    MockUser admin = new MockUser('admin', true)
    @Shared
    MockUser s1mUser = new MockUser('s1mUser', [study1: MEASUREMENTS])
    @Shared
    MockUser s1ctUser = new MockUser('s1ctUser', [study1: COUNTS_WITH_THRESHOLD])
    @Shared
    MockUser s2sUser = new MockUser('s2sUser', [study2: SUMMARY])
    @Shared
    MockUser s1sS2sUser = new MockUser('s1sS2sUser', [study1: SUMMARY, study2: SUMMARY])
    @Shared
    MockUser publicUser = new MockUser('publicUser', [:])

    @Shared
    Constraint trueConstraint = new TrueConstraint()
    @Shared
    Constraint study1Constraint = new StudyNameConstraint('study1')
    @Shared
    Constraint study1OnlySubjects = new PatientSetConstraint(subjectIds: ['Subject 1'])
    @Shared
    Constraint concept1Value1 = new AndConstraint([
            new ConceptConstraint('categorical_concept1'),
            new ValueConstraint(Type.STRING, Operator.EQUALS, 'value1')])
    @Shared
    Constraint concept1Value2 = new AndConstraint([
            new ConceptConstraint('categorical_concept1'),
            new ValueConstraint(Type.STRING, Operator.EQUALS, 'value2')])

    @Autowired
    SystemResource systemResource

    @Autowired
    AccessPolicyTestData accessPolicyTestData

    void setup() {
        accessPolicyTestData.clearTestData()
        accessPolicyTestData.createTestData()
        countsWithThresholdService.patientCountThreshold = 0
        //we keep changing threshold in these tests. Hence we need clean cache.
        systemResource.clearCaches()
    }

    @Unroll
    void 'test observations returned (POST .../observations) for #user.username.'() {

        given:
        selectUser(user)
        def body = new HypercubeRequest('clinical', trueConstraint)

        when:
        def response = post("${contextPath}/observations", body)

        then:
        checkResponseStatus(response, OK, user)
        Hypercube result = toObject(response, Hypercube)
        !getObservationlessSubjectElements(result)
        getObservedStudyNames(result) == observedStudies as Set


        where:
        user       | observedStudies
        admin      | ['study1', 'study2', 'publicStudy']
        s1mUser    | ['study1', 'publicStudy']
        s1sS2sUser | ['publicStudy']
        s1ctUser   | ['publicStudy']
        s2sUser    | ['publicStudy']
        publicUser | ['publicStudy']
    }

    @Unroll
    void 'test observations table data (POST .../observations/table) for #user.username.'() {

        given:
        selectUser(user)
        def body = new DataTableRequest()
        body.type = 'clinical'
        body.constraint = trueConstraint
        body.rowDimensions = ['patient']
        body.columnDimensions = ['study', 'concept']
        body.limit = 100

        when:
        def response = post("${contextPath}/observations/table", body)

        then:
        checkResponseStatus(response, OK, user)
        DataTable result = toObject(response, DataTable)
        result.columnDimensions.size() == 2
        DimensionRequest studyDim = result.columnDimensions.find { it.name == 'study' }
        Set<String> studyIds = studyDim.elements.keySet()
        studyIds == expectedStudies as Set<String>
        DimensionRequest patientDim = result.rowDimensions.find { it.name == 'patient' }
        patientDim.elements.keySet() == expectedSubjects as Set<String>
        Set<String> subjectsFromRowHeaders = result.rows.collect { row -> row.rowHeaders.find { el -> el.dimension == 'patient' }.key } as Set<String>
        subjectsFromRowHeaders == expectedSubjects as Set<String>

        where:
        user       | expectedStudies                     | expectedSubjects
        admin      | ['study1', 'study2', 'publicStudy'] | ['1/Subject 1', '2/Subject 2', '3/Subject 3', '4/Subject from public study']
        s1mUser    | ['study1', 'publicStudy']           | ['1/Subject 1', '2/Subject 2', '4/Subject from public study']
        s1sS2sUser | ['publicStudy']                     | ['4/Subject from public study']
        s1ctUser   | ['publicStudy']                     | ['4/Subject from public study']
        s2sUser    | ['publicStudy']                     | ['4/Subject from public study']
        publicUser | ['publicStudy']                     | ['4/Subject from public study']
    }


    @Unroll
    void 'test aggregates (POST .../observations/aggregates_per_concept) for #user.username.'() {
        given:
        Constraint constraint = trueConstraint
        def url = "${contextPath}/observations/aggregates_per_concept"
        selectUser(user)
        def body = new ConstraintHolder(constraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        Aggregates result = toObject(response, Aggregates)
        def counts = result.aggregatesPerConcept.collectEntries { key, value ->
            [
                    key,
                    value.categoricalValueAggregates ?
                            value.categoricalValueAggregates.valueCounts
                            : value.numericalValueAggregates.count
            ]
        }
        counts == visibleConcepts

        where:
        user       | visibleConcepts
        admin      | ['numerical_concept2': 3, 'categorical_concept1': [value1: 2, value2: 2, value3: 1, value4: 1]]
        s1sS2sUser | ['numerical_concept2': 3, 'categorical_concept1': [value1: 2, value2: 2, value3: 1, value4: 1]]
        s1mUser    | ['numerical_concept2': 2, 'categorical_concept1': [value1: 2, value2: 2, value3: 1]]
        s2sUser    | ['numerical_concept2': 1, 'categorical_concept1': [value1: 1, value4: 1]]
        s1ctUser   | ['categorical_concept1': [value1: 1]]
        publicUser | ['categorical_concept1': [value1: 1]]
    }

    /**
     * Tests the /observations/counts endpoint at {@link org.transmartproject.rest.QueryController#counts()}.
     */
    @Unroll
    void 'test counts (POST .../observations/counts) for #user.username and #constraint constraint.'() {
        given:
        def url = "${contextPath}/observations/counts"
        selectUser(user)
        def body = new ConstraintHolder(constraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        Counts counts = toObject(response, Counts)
        counts.patientCount == patientCount

        where:
        constraint       | user       | patientCount
        trueConstraint   | admin      | 4
        trueConstraint   | s1sS2sUser | 4
        trueConstraint   | s1mUser    | 3
        trueConstraint   | s2sUser    | 3
        trueConstraint   | s1ctUser   | 3
        trueConstraint   | publicUser | 1

        study1Constraint | admin      | 2
        study1Constraint | s1mUser    | 2
        study1Constraint | s1sS2sUser | 2
    }

    @Unroll
    void 'test counts per study (POST .../observations/counts_per_study) for #user.username.'() {
        given:
        def constraint = trueConstraint
        def url = "${contextPath}/observations/counts_per_study"
        selectUser(user)
        def body = new ConstraintHolder(constraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        CountsPerStudy counts = toObject(response, CountsPerStudy)
        counts.countsPerStudy.keySet() == studies as Set<String>

        where:
        user       | studies
        admin      | ['study1', 'study2', 'publicStudy']
        s1sS2sUser | ['study1', 'study2', 'publicStudy']
        s1mUser    | ['study1', 'publicStudy']
        s2sUser    | ['study2', 'publicStudy']
        s1ctUser   | ['study1', 'publicStudy']
        publicUser | ['publicStudy']
    }

    @Unroll
    void 'test counts per study and concept (POST .../observations/counts_per_study_and_concept) for #user.username.'() {
        given:
        def constraint = trueConstraint
        def url = "${contextPath}/observations/counts_per_study_and_concept"
        selectUser(user)
        def body = new ConstraintHolder(constraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        CountsPerStudyAndConcept counts = toObject(response, CountsPerStudyAndConcept)
        counts.countsPerStudy.keySet() == studies as Set<String>

        where:
        user       | studies
        admin      | ['study1', 'study2', 'publicStudy']
        s1sS2sUser | ['study1', 'study2', 'publicStudy']
        s1mUser    | ['study1', 'publicStudy']
        s2sUser    | ['study2', 'publicStudy']
        s1ctUser   | ['study1', 'publicStudy']
        publicUser | ['publicStudy']
    }

    /**
     * Tests the /observations/crosstable endpoint at {@link org.transmartproject.rest.QueryController#crosstable()}
     */
    @Unroll
    void 'test counts for crosstable (POST .../observations/crosstable) for #user.username with different patient data access levels.'() {

        given:
        def url = "${contextPath}/observations/crosstable"
        selectUser(user)
        def body = new CrossTableRequest(rowConstraints, columnConstraints, subjectConstraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        CrossTable table = toObject(response, CrossTable)
        table.rows == expectedValues

        where:
        rowConstraints   | columnConstraints | subjectConstraint | user       | expectedValues
        [trueConstraint] | [trueConstraint]  | trueConstraint    | admin      | [[4]]
        [trueConstraint] | [trueConstraint]  | trueConstraint    | s1sS2sUser | [[4]]
        [trueConstraint] | [trueConstraint]  | trueConstraint    | s1mUser    | [[3]]
        [trueConstraint] | [trueConstraint]  | trueConstraint    | s1ctUser   | [[3]]
        [trueConstraint] | [trueConstraint]  | trueConstraint    | s2sUser    | [[3]]
        [trueConstraint] | [trueConstraint]  | trueConstraint    | publicUser | [[1]]
    }

    @Unroll
    void 'test patients access (POST .../patients) for #user.username.'() {

        given:
        selectUser(user)
        def body = new ConstraintHolder(trueConstraint)

        when:
        def response = post("${contextPath}/patients", body)

        then:
        checkResponseStatus(response, OK, user)
        Map result = toObject(response, Map)
        result
        result.patients
        List<Map<String, String>> subjectIdMap = result.patients*.subjectIds
        Set<String> subjectIds = subjectIdMap.collect { it.get('SUBJ_ID') } as Set<String>
        subjectIds == expectedSubjectIds as Set<String>

        where:
        user       | expectedSubjectIds
        admin      | ['Subject 1', 'Subject 2', 'Subject 3', 'Subject from public study']
        s1mUser    | ['Subject 1', 'Subject 2', 'Subject from public study']
        s1sS2sUser | ['Subject from public study']
        s1ctUser   | ['Subject from public study']
        s2sUser    | ['Subject from public study']
        publicUser | ['Subject from public study']
    }

    @Unroll
    void 'test patient dimension elements access (POST .../dimensions/patient/elements) for #user.username.'() {

        given:
        selectUser(user)
        def body = new ConstraintHolder(trueConstraint)

        when:
        def response = post("${contextPath}/dimensions/patient/elements", body)

        then:
        checkResponseStatus(response, OK, user)
        Map result = toObject(response, Map)
        result
        result.elements
        List<Map<String, String>> subjectIdMap = result.elements*.subjectIds
        Set<String> subjectIds = subjectIdMap.collect { it.get('SUBJ_ID') } as Set<String>
        subjectIds == expectedSubjectIds as Set<String>

        where:
        user       | expectedSubjectIds
        admin      | ['Subject 1', 'Subject 2', 'Subject 3', 'Subject from public study']
        s1mUser    | ['Subject 1', 'Subject 2', 'Subject from public study']
        s1sS2sUser | ['Subject from public study']
        s1ctUser   | ['Subject from public study']
        s2sUser    | ['Subject from public study']
        publicUser | ['Subject from public study']
    }

    @Unroll
    void 'test observations forbidden access (POST .../observations) for #user.username when constraint explicitly refers to the protected resource.'() {

        given:
        selectUser(user)
        def body = new HypercubeRequest('clinical', study1Constraint)

        when:
        def response = post("${contextPath}/observations", body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        user << [s1ctUser, s2sUser, publicUser]
    }

    @Unroll
    void 'test observations table forbidden access (POST .../observations/table) for #user.username when constraint explicitly refers to the protected resource.'() {

        given:
        selectUser(user)
        def body = new DataTableRequest()
        body.type = 'clinical'
        body.constraint = study1Constraint
        body.limit = 100

        when:
        def response = post("${contextPath}/observations/table", body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        user << [s1ctUser, s2sUser, publicUser]
    }

    @Unroll
    void 'test observation aggregates forbidden access (POST .../observations/aggregates_per_concept) for #user.username when constraint explicitly refers to the protected resource.'() {

        given:
        selectUser(user)
        def body = new ConstraintHolder(study1Constraint)

        when:
        def response = post("${contextPath}/observations/aggregates_per_concept", body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        user << [s1ctUser, s2sUser, publicUser]
    }

    @Unroll
    void 'test observations crosstable forbidden access (POST .../observations/crosstable) for user when rowConstraints=#rowConstraints.constraintName, columnConstraints=#columnConstraints.constraintName and subjectConstraint=#subjectConstraint.constraintName.'() {

        given:
        def url = "${contextPath}/observations/crosstable"
        def user = s2sUser
        selectUser(user)
        def body = new CrossTableRequest(rowConstraints, columnConstraints, subjectConstraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        rowConstraints     | columnConstraints  | subjectConstraint
        [concept1Value1]   | [concept1Value2]   | study1Constraint
        [study1Constraint] | [concept1Value1]   | concept1Value2
        [concept1Value2]   | [study1Constraint] | concept1Value1
    }

    @Unroll
    void 'test observation counts forbidden access (POST ...#url) for user when constraint explicitly refers to the protected resource.'() {

        given:
        def user = s2sUser
        selectUser(user)
        def body = new ConstraintHolder(study1Constraint)

        when:
        def response = post("${contextPath}${url}", body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        url << [
                '/observations/counts',
                '/observations/counts_per_study',
                '/observations/counts_per_concept',
                '/observations/counts_per_study_and_concept']
    }

    @Unroll
    void 'test patients forbidden access (POST .../patients) for #user.username when constraint explicitly refers to the protected resource.'() {

        given:
        selectUser(user)
        def body = new ConstraintHolder(study1Constraint)

        when:
        def response = post("${contextPath}/patients", body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        user << [s1ctUser, s2sUser, publicUser]
    }

    @Unroll
    void 'test particular patient access (POST .../patients/#patientId) for #user.username when constraint explicitly refers to the protected resource.'() {

        given:
        selectUser(user)

        when:
        def response = get("${contextPath}/patients/${patientId}")

        then:
        checkResponseStatus(response, resultStatus, user)

        where:
        user       | patientId | resultStatus
        admin      | 1         | OK
        admin      | 3         | OK
        s1mUser    | 1         | OK
        s1mUser    | 3         | NOT_FOUND
        s1sS2sUser | 1         | NOT_FOUND
        s1sS2sUser | 3         | NOT_FOUND
        s1ctUser   | 1         | NOT_FOUND
        s1ctUser   | 3         | NOT_FOUND
        s2sUser    | 1         | NOT_FOUND
        s2sUser    | 3         | NOT_FOUND
        publicUser | 1         | NOT_FOUND
        publicUser | 3         | NOT_FOUND
    }

    @Unroll
    void 'test data export forbidden (POST .../export/...) for #user.username when constraint explicitly refers to the protected resource.'() {

        given:
        selectUser(user)

        when:
        def jobResponse = post("${contextPath}/export/job?name=testJobFor${user.username}", null)

        then:
        checkResponseStatus(jobResponse, OK, user)
        Map job = toObject(jobResponse, Map)
        job.exportJob
        job.exportJob.id

        when:
        def body = new ExportJobRepresentation(constraint: study1Constraint, elements:
                [new ExportElement(format: Format.TSV, dataType: 'clinical')])
        def response = post("${contextPath}/export/${job.exportJob.id}/run", body)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

        where:
        user << [s1ctUser, s2sUser, publicUser]
    }

    @Unroll
    void 'test patient sets forbidden access (POST .../patient_sets) for user when constraint explicitly refers to the protected resource.'() {

        given:
        def user = s2sUser
        selectUser(user)
        def constraint = study1Constraint

        when:
        def response = post("${contextPath}/patient_sets?name=test&reuse=false", constraint)

        then:
        checkResponseStatus(response, FORBIDDEN, user)
        def error = toObject(response, Map)
        error.message.startsWith('Access denied to study or study does not exist')

    }

    @Autowired
    CountsWithThresholdService countsWithThresholdService

    @Unroll
    void 'test counts (POST .../observations/counts) for constraint=#constraint and user with st1AccLvl=#s1AccLvl,  st2AccLvl=#s2AccLvl and threshold=#threshold.'() {
        given:
        def url = "${contextPath}/observations/counts"
        def user = new MockUser('counts with threshold user', [study1: s1AccLvl, study2: s2AccLvl])
        selectUser(user)
        countsWithThresholdService.patientCountThreshold = threshold
        def body = new ConstraintHolder(constraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        Counts counts = toObject(response, Counts)

        counts.patientCount == pateintCount
        counts.observationCount == observationCount

        where:
        constraint         | threshold | s1AccLvl              | s2AccLvl              | pateintCount | observationCount
        trueConstraint     | 1         | SUMMARY               | SUMMARY               | 4            | 9
        trueConstraint     | 1         | COUNTS_WITH_THRESHOLD | COUNTS_WITH_THRESHOLD | 4            | 9
        trueConstraint     | 5         | COUNTS_WITH_THRESHOLD | COUNTS_WITH_THRESHOLD | -2           | -2
        trueConstraint     | 5         | SUMMARY               | COUNTS_WITH_THRESHOLD | -2           | -2
        study1OnlySubjects | 5         | SUMMARY               | COUNTS_WITH_THRESHOLD | 1            | 4
    }

    @Unroll
    void 'test counts per concept threshold (POST .../observations/counts_per_concept) for user with st1AccLvl=#study1AccessLevel,  st2AccLvl=#study2AccessLevel and threshold=#threshold.'() {
        given:
        def url = "${contextPath}/observations/counts_per_concept"
        def user = new MockUser('counts with threshold user', [study1: study1AccessLevel, study2: study2AccessLevel])
        selectUser(user)
        countsWithThresholdService.patientCountThreshold = threshold
        def body = new ConstraintHolder(trueConstraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        CountsPerConcept counts = toObject(response, CountsPerConcept)
        Counts cat1Counts = counts.getCountsPerConcept().get('categorical_concept1')
        cat1Counts.patientCount == cat1PatientCount
        cat1Counts.observationCount == cat1ObservationCount
        Counts num2Counts = counts.getCountsPerConcept().get('numerical_concept2')
        num2Counts.patientCount == num2PatientCount
        num2Counts.observationCount == num2ObservationCount

        where:
        threshold | study1AccessLevel     | study2AccessLevel     | cat1PatientCount | cat1ObservationCount | num2PatientCount | num2ObservationCount
        5         | SUMMARY               | SUMMARY               | 4                | 6                    | 2                | 3
        2         | COUNTS_WITH_THRESHOLD | COUNTS_WITH_THRESHOLD | 4                | 6                    | 2                | 3
        3         | SUMMARY               | COUNTS_WITH_THRESHOLD | 4                | 6                    | -2               | -2
        5         | COUNTS_WITH_THRESHOLD | COUNTS_WITH_THRESHOLD | -2               | -2                   | -2               | -2
    }

    @Unroll
    void 'test counts per study threshold (POST .../observations/counts_per_study) for user with st1AccLvl=#study1AccessLevel,  st2AccLvl=#study2AccessLevel and threshold=#threshold.'() {
        given:
        def url = "${contextPath}/observations/counts_per_study"
        def user = new MockUser('counts with threshold user', [study1: study1AccessLevel, study2: study2AccessLevel])
        selectUser(user)
        countsWithThresholdService.patientCountThreshold = threshold
        def body = new ConstraintHolder(trueConstraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        CountsPerStudy counts = toObject(response, CountsPerStudy)
        Counts study1Counts = counts.getCountsPerStudy().get('study1')
        study1Counts.patientCount == study1PatientCount
        study1Counts.observationCount == study1ObservationCount
        Counts study2Counts = counts.getCountsPerStudy().get('study2')
        study2Counts.patientCount == study2PatientCount
        study2Counts.observationCount == study2ObservationCount
        Counts publicStudyCounts = counts.getCountsPerStudy().get('publicStudy')
        publicStudyCounts.patientCount == 1
        publicStudyCounts.observationCount == 1

        where:
        threshold | study1AccessLevel     | study1PatientCount | study1ObservationCount | study2AccessLevel     | study2PatientCount | study2ObservationCount
        3         | SUMMARY               | 2                  | 6                      | SUMMARY               | 2                  | 2
        2         | COUNTS_WITH_THRESHOLD | 2                  | 6                      | COUNTS_WITH_THRESHOLD | 2                  | 2
        3         | SUMMARY               | 2                  | 6                      | COUNTS_WITH_THRESHOLD | -2                 | -2
        3         | COUNTS_WITH_THRESHOLD | -2                 | -2                     | COUNTS_WITH_THRESHOLD | -2                 | -2
    }

    @Unroll
    void 'test counts per study and concept threshold (POST .../observations/counts_per_study_and_concept) for user with st1AccLvl=#s1AccLvl,  st2AccLvl=#s2AccLvl and threshold=#threshold.'() {
        given:
        def url = "${contextPath}/observations/counts_per_study_and_concept"
        def user = new MockUser('counts with threshold user', [study1: s1AccLvl, study2: s2AccLvl])
        selectUser(user)
        countsWithThresholdService.patientCountThreshold = threshold
        def body = new ConstraintHolder(trueConstraint)

        when:
        def response = post(url, body)

        then:
        checkResponseStatus(response, OK, user)
        CountsPerStudyAndConcept counts = toObject(response, CountsPerStudyAndConcept)

        Map<String, Counts> study1PerConceptCounts = counts.getCountsPerStudy().get('study1')
        study1PerConceptCounts == [categorical_concept1: new Counts(patientCount: s1Cat1Pat, observationCount: s1Cat1Obs),
                                   numerical_concept2  : new Counts(patientCount: s1Num1Pat, observationCount: s1Num1Obs)]

        Map<String, Counts> study2PerConceptCounts = counts.getCountsPerStudy().get('study2')
        study2PerConceptCounts == [categorical_concept1: new Counts(patientCount: s2Cat1Pat, observationCount: s2Cat1Obs),
                                   numerical_concept2  : new Counts(patientCount: s2Num1Pat, observationCount: s2Num1Obs)]

        Map<String, Counts> publicStudyPerConceptCounts = counts.getCountsPerStudy().get('publicStudy')
        publicStudyPerConceptCounts == [categorical_concept1: new Counts(patientCount: 1, observationCount: 1)]

        where:
        threshold | s1AccLvl              | s1Cat1Pat | s1Cat1Obs | s1Num1Pat | s1Num1Obs | s2AccLvl              | s2Cat1Pat | s2Cat1Obs | s2Num1Pat | s2Num1Obs
        2         | SUMMARY               | 2         | 4         | 1         | 2         | SUMMARY               | 1         | 1         | 1         | 1
        2         | COUNTS_WITH_THRESHOLD | 2         | 4         | -2        | -2        | COUNTS_WITH_THRESHOLD | -2        | -2        | -2        | -2
        2         | COUNTS_WITH_THRESHOLD | 2         | 4         | -2        | -2        | SUMMARY               | 1         | 1         | 1         | 1
        3         | COUNTS_WITH_THRESHOLD | -2        | -2        | -2        | -2        | COUNTS_WITH_THRESHOLD | -2        | -2        | -2        | -2
    }

    /**
     * Get study names from observations.
     * @param hypercube
     * @return
     */
    Set<String> getObservedStudyNames(Hypercube hypercube) {
        def studyDimesnionIndex = hypercube.dimensionDeclarations.findIndexOf { it.name == 'study' && !it.inline }
        assert studyDimesnionIndex >= 0: 'No study dimension among non-inline dimensions of the hypercube.'
        Set<Integer> studyElementIndexes = hypercube.cells.collect {
            it.dimensionIndexes[studyDimesnionIndex]
        } as Set<Integer>
        if (!studyElementIndexes) {
            return [] as Set<String>
        }
        List<Map<String, Object>> studyElements = hypercube.dimensionElements['study']
        studyElementIndexes.collect { Integer indx ->
            assert indx != null: 'Study index could not be null.'
            studyElements.get(indx).name
        } as Set<String>
    }

    /**
     * Used to check that we don't expose extra patient information.
     * @param hypercube
     * @return
     */
    def getObservationlessSubjectElements(Hypercube hypercube) {
        def patientDimensionIndex = hypercube.dimensionDeclarations.findIndexOf { it.name == 'patient' && !it.inline }
        assert patientDimensionIndex >= 0: 'No patient dimension among non-inline dimensions of the hypercube.'
        Set<Integer> patientElementIndexes = hypercube.cells.collect {
            it.dimensionIndexes[patientDimensionIndex]
        } as Set<Integer>
        List<Map<String, Object>> patientElementsCopy = new ArrayList(hypercube.dimensionElements['patient'])
        List<Integer> indxToRemove = patientElementIndexes.sort().reverse()

        indxToRemove.each { Integer indx ->
            assert indx != null: 'Subject index could not be null.'
            patientElementsCopy.remove(indx)
        }
        return patientElementsCopy
    }

    @Unroll
    void 'test dimension elements access (POST .../dimensions/#dimension/elements) for #user.username.'(
            MockUser user, String dimension, HttpStatus expectedStatus
    ) {
        given:
        def url = "${contextPath}/dimensions/${dimension}/elements"
        def constraint = study1Constraint

        when:
        selectUser(user)
        def body = new ConstraintHolder(constraint)
        def response = post(url, body)

        then:
        checkResponseStatus(response, expectedStatus, user)

        where:
        user       | dimension     | expectedStatus
        s1mUser    | 'patient'     | OK
        s1mUser    | 'concept'     | OK
        s1mUser    | 'study'       | OK
        s1mUser    | 'trial visit' | OK
        s1ctUser   | 'patient'     | FORBIDDEN
        s1ctUser   | 'concept'     | OK
        s1ctUser   | 'study'       | OK
        s1ctUser   | 'trial visit' | OK
        s1sS2sUser | 'patient'     | FORBIDDEN
    }

}
