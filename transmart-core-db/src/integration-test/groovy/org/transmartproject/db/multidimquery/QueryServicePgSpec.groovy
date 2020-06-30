/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.ModifierConstraint
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.TimeConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.multidimquery.query.ValueConstraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.clinical.PatientSetService
import org.transmartproject.db.querytool.QtPatientSetCollection
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.user.User
import spock.lang.Specification

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.multidimquery.DimensionImpl.*
import static spock.util.matcher.HamcrestSupport.that

@Rollback
@Integration
class QueryServicePgSpec extends Specification {

    public static final String DATE_TIME_FORMAT = 'yyyy-MM-dd HH:mm:ss'
    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    AggregateDataResource aggregateDataResource

    @Autowired
    PatientSetService patientSetResource

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_TIME_FORMAT)
    private static final DateFormat UTC_DATE_FORMAT
    static {
        UTC_DATE_FORMAT = new SimpleDateFormat(DATE_TIME_FORMAT)
        UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone('UTC'))
    }

    void 'Clinical data selected on visit dimension'() {
        def user = User.findByUsername('test-public-user-1')
        def minDate = DATE_FORMAT.parse('2016-04-01 10:00:00')
        def visitStartConstraint = new FieldConstraint(
                operator: Operator.AFTER,
                value: minDate,
                field: new Field(
                        dimension: VISIT.name,
                        fieldName: 'startDate',
                        type: 'DATE'
                )
        )
        def studyNameConstraint = new StudyNameConstraint(
                studyId: 'EHR'
        )
        def combination = new Combination(Operator.AND, [visitStartConstraint, studyNameConstraint])
        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(combination, user)
        def observations = hypercube.toList()

        then:
        observations.size() == 2
        hypercube.dimensionElements(VISIT).each {
            assert (it.getAt('startDate') as Date) > minDate
        }

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'HD data selected based on sample type (modifier)'() {
        def user = User.findByUsername('test-public-user-1')
        def modifierConstraint = new ModifierConstraint(
                modifierCode: 'TNS:SMPL',
                //path: '\\Public Studies\\TUMOR_NORMAL_SAMPLES\\Sample Type\\', //choose either code or path
                values: new ValueConstraint(
                        operator: Operator.EQUALS,
                        valueType: Type.STRING,
                        value: 'Tumor'

                )
        )
        def conceptConstraint = new ConceptConstraint(
                path: '\\Public Studies\\TUMOR_NORMAL_SAMPLES\\HD\\Breast\\'
        )

        def combination = new Combination(
                Operator.AND,
                [modifierConstraint, conceptConstraint]
        )
        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(combination, user)
        hypercube.toList()

        then:
        hypercube.dimensionElements(PATIENT).size() == 2

        cleanup:
        if (hypercube) hypercube.close()
    }

    void 'get data for the patient with given id'() {
        def user = User.findByUsername('test-public-user-1')
        Constraint subjectIdConstraint = new PatientSetConstraint(subjectIds: ['2'])


        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(subjectIdConstraint, user)
        then:
        hypercube.dimensionElements(PATIENT).size() == 1
        hypercube.dimensionElements(PATIENT).first().subjectIds.get('SUBJ_ID') == '2'

        cleanup:
        if (hypercube) hypercube.close()
    }

    // This test should be a part of QueryServiceSpec tests in transmart-core-db-tests.
    // The same issue as for the test above: since we cannot run that one due to limitations in the H2 database
    // this version ensures that the functionality is still automatically tested.
    void "test query for 'visit' dimension elements"() {
        def user = User.findByUsername('test-public-user-1')
        DimensionImpl dimension = VISIT

        Constraint constraint = new StudyNameConstraint(studyId: "EHR")
        def results = multiDimService.retrieveClinicalData(constraint, user).asList()
        def expectedResult = results.collect { it[VISIT] }.findAll { it } as Set

        when: "I query for all visits for a constraint"
        def visits = multiDimService.getDimensionElements(dimension.name, constraint, user).collect {
            dimension.asSerializable(it)
        }

        then: "List of all visits matching the constraints is returned"
        visits.size() == expectedResult.size()
        visits.collect { it.encounterIds['VISIT_ID'] }.sort() == expectedResult.collect { it.encounterIds['VISIT_ID']}.sort()
    }

    void "test query all observations"() {
        def user = User.findByUsername('test-public-user-1')

        when: "I enumerate all observations"
        def results = multiDimService.retrieveClinicalData(new TrueConstraint(), user).asList()

        then: "No errors occur and the observation count is non-zero"
        results.size() > 0
    }

    void "test patient set query"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        when:
        QueryResult patientSetQueryResult = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        then:
        patientSetQueryResult.id > 0
        patientSetQueryResult.queryResultType.id == QueryResultType.PATIENT_SET_ID
        patientSetQueryResult.setSize == 3L
        patientSetQueryResult.status == QueryStatus.FINISHED
        patientSetQueryResult.username == user.name
        !patientSetQueryResult.description.empty
        patientSetQueryResult.errorMessage == null
        def patientSetEntries = QtPatientSetCollection
                .findAllByResultInstance(QtQueryResultInstance.load(patientSetQueryResult.id))
        patientSetEntries.size() == 3
        patientSetEntries*.patient.id != null
    }

    void "reuse patient set query if one with similar constraints already exists for the user"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        QueryResult patientSetQueryResult1 = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        when:
        QueryResult patientSetQueryResult2 = patientSetResource.createPatientSetQueryResult("Test set 2",
                constraint,
                user,
                'v2',
                true)

        then:
        patientSetQueryResult1
        patientSetQueryResult1 == patientSetQueryResult2
    }

    void "test a new patient set created even there is possibility to reuse"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        QueryResult patientSetQueryResult1 = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        when:
        QueryResult patientSetQueryResult2 = patientSetResource.createPatientSetQueryResult("Test set 2",
                constraint,
                user,
                'v2',
                false)

        then:
        patientSetQueryResult1
        patientSetQueryResult1 != patientSetQueryResult2
    }

    void "test reusing patient set query after the patient set ids cache cleanup"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(path:
                '\\Public Studies\\CLINICAL_TRIAL_HIGHDIM\\High Dimensional data\\Expression Lung\\')

        QueryResult patientSetQueryResult1 = patientSetResource.createPatientSetQueryResult("Test set",
                constraint,
                user,
                'v2',
                false)

        when:
        patientSetResource.clearPatientSetIdsCache()
        QueryResult patientSetQueryResult2 = patientSetResource.createPatientSetQueryResult("Test set 2",
                constraint,
                user,
                'v2',
                true)

        then:
        patientSetQueryResult1
        patientSetQueryResult1 != patientSetQueryResult2
    }

    void "test large text values (raw data type)"() {
        def user = User.findByUsername('test-public-user-1')

        ConceptConstraint constraint = new ConceptConstraint(conceptCode: 'favouritebook')

        when:
        Counts counts = aggregateDataResource.counts(constraint, user)
        Hypercube data = multiDimService.retrieveClinicalData(constraint, user)
        def values = data.collect { HypercubeValue value -> value.value as String }

        then:
        counts.observationCount == 3
        that values, hasSize(3)
        that values, hasItems(containsString('The Brothers Karamazov'), containsString('funny dialogues'))
    }

    void "test searching for large text values (raw data type)"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new AndConstraint([
                new ConceptConstraint(conceptCode: 'favouritebook'),
                new ValueConstraint(Type.TEXT, Operator.CONTAINS, 'Karamazov')
        ])

        when:
        Counts counts = aggregateDataResource.counts(constraint, user)
        Hypercube data = multiDimService.retrieveClinicalData(constraint, user)
        def values = data.collect { HypercubeValue value -> value.value as String }

        then:
        counts.observationCount == 1
        that values, hasSize(1)
        that values, hasItems(containsString('The Brothers Karamazov'))
    }

    void "test time values constraint"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new AndConstraint(
                [
                        new ConceptConstraint(path: '\\Demographics\\Birth Date\\'),
                        new TimeConstraint(
                            field: new Field(
                                    dimension: VALUE.name,
                                    //TODO Has to fail after TMT-420
                                    fieldName: 'numberValue',
                                    type: Type.DATE
                            ),
                            values: [DATE_FORMAT.parse('1986-10-21 00:00:00'), DATE_FORMAT.parse('1986-10-23 00:00:00')],
                            operator: Operator.BETWEEN
                        )
                ]
        )

        when:
        def values = multiDimService.retrieveClinicalData(constraint, user).asList()*.value

        then:
        values.size() == 1
        values[0] instanceof Date
        values[0] == UTC_DATE_FORMAT.parse('1986-10-22 00:00:00')
    }

    void "test values of date type"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new AndConstraint([
                new StudyNameConstraint(studyId: 'SURVEY1'),
                new ValueConstraint(valueType: Type.DATE, operator: Operator.AFTER,
                        value: DATE_FORMAT.parse('1986-10-21 00:00:00'))
        ])

        when:
        def values = multiDimService.retrieveClinicalData(constraint, user).asList()*.value
        values.sort()

        then:
        values.size() == 2
        values[0] instanceof Date
        values[0] == UTC_DATE_FORMAT.parse('1986-10-22 00:00:00')
        values[1] instanceof Date
        values[1] == UTC_DATE_FORMAT.parse('2001-09-01 05:30:05')
    }

    void 'test numerical constraints'() {
        given: 'a constraint for temperature readings'
        def user = User.findByUsername('test-public-user-2')

        Constraint temperature = new ConceptConstraint('VSIGN:TEMP')

        when: 'retrieving aggregates for the concept'
        def aggregates = aggregateDataResource.numericalValueAggregatesPerConcept(temperature, user)

        then: 'the aggregates match expected values'
        aggregates['VSIGN:TEMP'].count == 7
        aggregates['VSIGN:TEMP'].min == 55
        aggregates['VSIGN:TEMP'].max == 89

        when: 'restricting to values < 82'
        Constraint lessThan = new AndConstraint([temperature, new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN, 82)])
        def aggregates2 = aggregateDataResource.numericalValueAggregatesPerConcept(lessThan, user)

        then: 'the aggregates differ from the previous accordingly'
        aggregates2['VSIGN:TEMP'].count == 4
        aggregates2['VSIGN:TEMP'].min == 55
        aggregates2['VSIGN:TEMP'].max == 81

        when: 'restricting to values <= 82'
        Constraint lessThanOrEquals = new AndConstraint([temperature, new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN_OR_EQUALS, 82)])
        def aggregates3 = aggregateDataResource.numericalValueAggregatesPerConcept(lessThanOrEquals, user)

        then: 'the aggregates differ from the previous accordingly'
        aggregates3['VSIGN:TEMP'].count == 6
        aggregates3['VSIGN:TEMP'].min == 55
        aggregates3['VSIGN:TEMP'].max == 82
    }

}
