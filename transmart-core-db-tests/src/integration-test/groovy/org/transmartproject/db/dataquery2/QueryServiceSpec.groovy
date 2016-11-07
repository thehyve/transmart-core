package org.transmartproject.db.dataquery2

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.dataquery2.query.Combination
import org.transmartproject.db.dataquery2.query.ConceptConstraint
import org.transmartproject.db.dataquery2.query.Constraint
import org.transmartproject.db.dataquery2.query.ConstraintFactory
import org.transmartproject.db.dataquery2.query.InvalidQueryException
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.dataquery2.query.Operator
import org.transmartproject.db.dataquery2.query.QueryType
import org.transmartproject.db.dataquery2.query.TrueConstraint
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.user.AccessLevelTestData
import org.transmartproject.db.i2b2data.ConceptDimension

@Rollback
@Integration
class QueryServiceSpec extends TransmartSpecification {

    @Autowired
    QueryService queryService

    TestData testData
    AccessLevelTestData accessLevelTestData

    void setupData() {
        testData = new TestData().createDefault()
        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.i2b2Data.patients[2].age = 18
        accessLevelTestData = new AccessLevelTestData().createWithAlternativeConceptData(testData.conceptData)
        testData.saveAll()
        accessLevelTestData.saveAll()
    }

    ObservationQuery createQueryForConcept(ObservationFact observationFact){
        def conceptCode = observationFact.conceptCode
        def conceptDimension = ConceptDimension.find {
            conceptCode == conceptCode
        }
        ObservationQuery query = new ObservationQuery(
                constraint: new ConceptConstraint(
                        path: conceptDimension.conceptPath
                )
        )
        query
    }

    ObservationFact createFactForExistingConcept(){
        def clinicalTestdata = testData.clinicalData
        def fact = clinicalTestdata.facts.find { it.valueType=='N'}
        def conceptDimension = testData.conceptData.conceptDimensions.find{ it.conceptCode ==fact.conceptCode}
        def patientsWithConcept = clinicalTestdata.facts.collect {
            if(it.conceptCode == conceptDimension.conceptCode){
                it.patient
            }
        }
        def patientDimension = clinicalTestdata.patients.find {
            !patientsWithConcept.contains(it)
        }

        ObservationFact observationFact = clinicalTestdata.createObservationFact(
                conceptDimension, patientDimension, clinicalTestdata.DUMMY_ENCOUNTER_ID, -1
        )

        observationFact
    }

    void "test query for all observations"() {
        setupData()

        TrueConstraint constraint = new TrueConstraint()
        ObservationQuery query = new ObservationQuery(
                constraint: constraint,
                queryType: QueryType.VALUES
        )

        when:
        def result = queryService.list(query, accessLevelTestData.users[0])

        then:
        result.size() == 4
    }

    void "test query for values > 1 and subject id 2"() {
        setupData()

        Constraint constraint = ConstraintFactory.create([
                type: 'Combination',
                operator: 'and',
                args: [
                        [
                                type: 'ValueConstraint',
                                valueType: 'NUMERIC',
                                operator: '>',
                                value: 1
                        ],
                        [
                                type: 'FieldConstraint',
                                field: [dimension: 'PatientDimension', fieldName: 'sourcesystemCd'],
                                operator: 'contains',
                                value: 'SUBJ_ID_2'
                        ]
                ]
        ])
        ObservationQuery query = new ObservationQuery(
                constraint: constraint,
                queryType: QueryType.VALUES
        )

        when:
        def observations = ObservationFact.findAll {
            valueType == ObservationFact.TYPE_NUMBER
            numberValue > 1
            createAlias('patient', 'p')
            like('p.sourcesystemCd', '%SUBJ_ID_2%')
        }
        def result = queryService.list(query, accessLevelTestData.users[0])

        then:
        result.size() == observations.size()
        result.size() == 1
        result[0].valueType == ObservationFact.TYPE_NUMBER
        result[0].numberValue > 1
        result[0].patient.sourcesystemCd.contains('SUBJ_ID_2')
    }

    void "test for max, min, average aggregate"(){
        setupData()

        ObservationFact observationFact = createFactForExistingConcept()
        observationFact.numberValue = 50
        testData.clinicalData.facts << observationFact

        testData.saveAll()
        def query = createQueryForConcept(observationFact)

        when:
        query.queryType = QueryType.MAX
        def result = queryService.aggregate(query, accessLevelTestData.users[0])

        then:
        result == 50

        when:
        query.queryType = QueryType.MIN
        result = queryService.aggregate(query, accessLevelTestData.users[0])

        then:
        result == 10

        when:
        query.queryType = QueryType.AVERAGE
        result = queryService.aggregate(query, accessLevelTestData.users[0])

        then:
        result == 30 //(10+50) / 2
    }

    void "test for check if aggregate returns error when any numerical value is null"(){
        setupData()

        def observationFact = createFactForExistingConcept()

        observationFact.numberValue = null
        observationFact.textValue='E'
        observationFact.valueType='N'
        testData.clinicalData.facts << observationFact
        testData.saveAll()

        when:
        ObservationQuery query = createQueryForConcept(observationFact)
        query.queryType = QueryType.MAX
        queryService.aggregate(query, accessLevelTestData.users[0])

        then:
        thrown(InvalidQueryException)

    }

    void "test for check if aggregate returns error when any textValue is other then E"(){
        setupData()

        def observationFact = createFactForExistingConcept()
        observationFact.textValue = 'GT'
        observationFact.numberValue = 60
        testData.clinicalData.facts << observationFact
        testData.saveAll()

        when:
        ObservationQuery query = createQueryForConcept(observationFact)
        query.queryType = QueryType.MAX
        queryService.aggregate(query, accessLevelTestData.users[0])

        then:
        thrown(InvalidQueryException)
    }

    void "test correct conceptConstraint checker in aggregate function"() {
        setup:
        setupData()

        def user = accessLevelTestData.users[0]
        def fact = testData.clinicalData.facts.find { it.valueType=='N'}
        def conceptDimension = testData.conceptData.conceptDimensions.find{ it.conceptCode ==fact.conceptCode}

        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.MAX
        )

        when:
        query.constraint = new TrueConstraint()
        queryService.aggregate(query, user)

        then:
        thrown(InvalidQueryException)

        when:
        query.constraint = new Combination(
                operator: Operator.AND,
                args:[
                        new TrueConstraint(),
                        new ConceptConstraint(
                                path: conceptDimension.conceptPath
                        ),
                        new Combination(
                                operator: Operator.AND,
                                args:[
                                        new ConceptConstraint(
                                                path:conceptDimension.conceptPath
                                        ),
                                        new TrueConstraint()
                                ]
                        )
                ]
        )

        queryService.aggregate(query, user)

        then:
        thrown(InvalidQueryException)

        when:
        def firstConceptConstraint = query.constraint.args.find{ it.class == ConceptConstraint}
        query.constraint.args = query.constraint.args - firstConceptConstraint
        def result = queryService.aggregate(query, user)

        then:
        result == 10

    }

}
