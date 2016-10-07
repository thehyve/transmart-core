package org.transmartproject.db.dataquery2

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.dataquery2.query.Constraint
import org.transmartproject.db.dataquery2.query.ConstraintFactory
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.dataquery2.query.QueryType
import org.transmartproject.db.dataquery2.query.TrueConstraint
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
        testData = new TestData().createDefault()
        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.i2b2Data.patients[2].age = 18
        accessLevelTestData = new AccessLevelTestData().createWithAlternativeConceptData(testData.conceptData)
        testData.saveAll()
        accessLevelTestData.saveAll()
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
                                operator: 'in',
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

}
