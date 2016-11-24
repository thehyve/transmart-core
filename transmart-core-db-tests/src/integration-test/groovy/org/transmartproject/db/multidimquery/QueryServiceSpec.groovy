package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.user.AccessLevelTestData

@Rollback
@Integration
class QueryServiceSpec extends TransmartSpecification {

    @Autowired
    QueryService queryService

    TestData testData
    AccessLevelTestData accessLevelTestData

    void setupData() {
        testData = new TestData().createDefault(); int i = 1
        testData.mrnaData.patients = testData.i2b2Data.patients

        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.i2b2Data.patients[2].age = 18
        accessLevelTestData = new AccessLevelTestData().createWithAlternativeConceptData(testData.conceptData)
        testData.saveAll()
        accessLevelTestData.saveAll()
    }

    Constraint createQueryForConcept(ObservationFact observationFact) {
        def conceptCode = observationFact.conceptCode
        def conceptDimension = ConceptDimension.find {
            conceptCode == conceptCode
        }
        new ConceptConstraint(path: conceptDimension.conceptPath)
    }

    ObservationFact createFactForExistingConcept() {
        def clinicalTestdata = testData.clinicalData
        def fact = clinicalTestdata.facts.find { it.valueType == 'N' }
        def conceptDimension = testData.conceptData.conceptDimensions.find { it.conceptCode == fact.conceptCode }
        def patientsWithConcept = clinicalTestdata.facts.collect {
            if (it.conceptCode == conceptDimension.conceptCode) {
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

        when:
        def result = queryService.list(constraint, accessLevelTestData.users[0])

        then:
        result.size() == 4
    }

    void "test query for values > 1 and subject id 2"() {
        setupData()

        Constraint constraint = ConstraintFactory.create([
                type    : 'Combination',
                operator: 'and',
                args    : [
                        [
                                type     : 'ValueConstraint',
                                valueType: 'NUMERIC',
                                operator : '>',
                                value    : 1
                        ],
                        [
                                type    : 'FieldConstraint',
                                field   : [dimension: 'PatientDimension', fieldName: 'sourcesystemCd'],
                                operator: 'contains',
                                value   : 'SUBJ_ID_2'
                        ]
                ]
        ])

        when:
        def observations = ObservationFact.findAll {
            valueType == ObservationFact.TYPE_NUMBER
            numberValue > 1
            createAlias('patient', 'p')
            like('p.sourcesystemCd', '%SUBJ_ID_2%')
        }
        def result = queryService.list(constraint, accessLevelTestData.users[0])

        then:
        result.size() == observations.size()
        result.size() == 1
        result[0].valueType == ObservationFact.TYPE_NUMBER
        result[0].numberValue > 1
        result[0].patient.sourcesystemCd.contains('SUBJ_ID_2')
    }

    void "test for max, min, average aggregate"() {
        setupData()

        ObservationFact observationFact = createFactForExistingConcept()
        observationFact.numberValue = 50
        testData.clinicalData.facts << observationFact

        testData.saveAll()
        def query = createQueryForConcept(observationFact)

        when:
        def result = queryService.aggregate(AggregateType.MAX, query, accessLevelTestData.users[0])

        then:
        result == 50

        when:
        result = queryService.aggregate(AggregateType.MIN, query, accessLevelTestData.users[0])

        then:
        result == 10

        when:
        result = queryService.aggregate(AggregateType.AVERAGE, query, accessLevelTestData.users[0])

        then:
        result == 30 //(10+50) / 2
    }

    void "test for check if aggregate returns error when any numerical value is null"() {
        setupData()

        def observationFact = createFactForExistingConcept()

        observationFact.numberValue = null
        observationFact.textValue = 'E'
        observationFact.valueType = 'N'
        testData.clinicalData.facts << observationFact
        testData.saveAll()

        when:
        Constraint query = createQueryForConcept(observationFact)
        queryService.aggregate(AggregateType.MAX, query, accessLevelTestData.users[0])

        then:
        thrown(InvalidQueryException)

    }

    void "test for check if aggregate returns error when any textValue is other then E"() {
        setupData()

        def observationFact = createFactForExistingConcept()
        observationFact.textValue = 'GT'
        observationFact.numberValue = 60
        testData.clinicalData.facts << observationFact
        testData.saveAll()

        when:
        Constraint query = createQueryForConcept(observationFact)
        queryService.aggregate(AggregateType.MAX, query, accessLevelTestData.users[0])

        then:
        thrown(InvalidQueryException)
    }

    void "test correct conceptConstraint checker in aggregate function"() {
        setup:
        setupData()

        def user = accessLevelTestData.users[0]
        def fact = testData.clinicalData.facts.find { it.valueType == 'N' }
        def conceptDimension = testData.conceptData.conceptDimensions.find { it.conceptCode == fact.conceptCode }

        when:
        def constraint = new TrueConstraint()
        queryService.aggregate(AggregateType.MAX, constraint, user)

        then:
        thrown(InvalidQueryException)

        when:
        constraint = new Combination(
                operator: Operator.AND,
                args: [
                        new TrueConstraint(),
                        new ConceptConstraint(
                                path: conceptDimension.conceptPath
                        ),
                        new Combination(
                                operator: Operator.AND,
                                args: [
                                        new ConceptConstraint(
                                                path: conceptDimension.conceptPath
                                        ),
                                        new TrueConstraint()
                                ]
                        )
                ]
        )

        queryService.aggregate(AggregateType.MAX, constraint, user)

        then:
        thrown(InvalidQueryException)

        when:
        def firstConceptConstraint = constraint.args.find { it.class == ConceptConstraint }
        constraint.args = constraint.args - firstConceptConstraint
        def result = queryService.aggregate(AggregateType.MAX, constraint, user)

        then:
        result == 10

    }

    void 'get whole hd data for single node'() {
        setup:
        setupData()
        def user = accessLevelTestData.users[0]
        ConceptConstraint constraint = new ConceptConstraint(path: '\\foo\\study1\\bar\\')
        String projection = Projection.DEFAULT_REAL_PROJECTION

        when:
        def (projectionObj, result) = queryService.highDimension(constraint, null, null, projection, user)

        then:
        projectionObj instanceof Projection
        result instanceof TabularResult
        result.rows.size() == 3
        result.indicesList.size() == 3
    }

    void 'get hd data for selected patients'() {
        setup:
        setupData()
        def user = accessLevelTestData.users[0]
        ConceptConstraint constraint = new ConceptConstraint(path: '\\foo\\study1\\bar\\')
        String projection = Projection.DEFAULT_REAL_PROJECTION

        when:
        Constraint assayConstraint = new PatientSetConstraint(patientIds: [testData.i2b2Data.patients[0].id] as Set)
        def (projectionObj, result) = queryService.highDimension(constraint, null, assayConstraint, projection, user)

        then:
        projectionObj instanceof Projection
        result instanceof TabularResult
        result.rows.size() == 3
        result.indicesList.size() == 1
    }

    void 'get hd data for selected biomarkers'() {
        setup:
        setupData()
        def user = accessLevelTestData.users[0]
        ConceptConstraint constraint = new ConceptConstraint(path: '\\foo\\study1\\bar\\')
        String projection = Projection.DEFAULT_REAL_PROJECTION

        when:
        BiomarkerConstraint bioMarkerConstraint = new BiomarkerConstraint(
                biomarkerType: DataConstraint.GENES_CONSTRAINT,
                params: [
                        names: ['BOGUSRQCD1']
                ]
        )
        def (projectionObj, result) = queryService.highDimension(constraint, bioMarkerConstraint, null, projection, user)

        then:
        projectionObj instanceof Projection
        result instanceof TabularResult
        result.rows.size() == 1
        result.indicesList.size() == 3
    }

}
