package org.transmartproject.db.dataquery2

import grails.converters.JSON
import grails.gorm.DetachedCriteria
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import org.transmartproject.db.TestData
import org.transmartproject.db.TransmartSpecification
import org.transmartproject.db.dataquery2.query.CriteriaQueryBuilder
import org.transmartproject.db.dataquery2.query.Field
import org.transmartproject.db.dataquery2.query.FieldConstraint
import org.transmartproject.db.dataquery2.query.Negation
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.dataquery2.query.Operator
import org.transmartproject.db.dataquery2.query.QueryBuilder
import org.transmartproject.db.dataquery2.query.QueryType
import org.transmartproject.db.dataquery2.query.TemporalConstraint
import org.transmartproject.db.dataquery2.query.TrueConstraint
import org.transmartproject.db.dataquery2.query.Type
import org.transmartproject.db.dataquery2.query.ValueConstraint
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study

@Slf4j
@Rollback
@Integration
class CriteriaQueryBuilderSpec extends TransmartSpecification {

    Field patientAgeField
    Field conceptCodeField
    TestData testData

    void setupData() {
        patientAgeField = new Field(dimension: PatientDimension.class, fieldName: 'age', type: Type.NUMERIC)
        conceptCodeField = new Field(dimension: ConceptDimension.class, fieldName: 'conceptCode', type: Type.STRING)

        testData = new TestData().createDefault()
        testData.i2b2Data.patients[0].age = 70
        testData.i2b2Data.patients[1].age = 31
        testData.saveAll()
    }

    void 'test GORM subqueries'() {
        setupData()

        when:
        DetachedCriteria criteria = ObservationFact.where {
            patient.age > 60
        }
        List result1 = criteria.list()
        log.info "RESULT: ${result1.asList()}"

        then:
        result1.size() == 1

        when:
        def query = ObservationFact.where {
            def o1 = ObservationFact
            startDate < ObservationFact.where {
                criteria.criteria.each { add(it) }
                def o2 = ObservationFact
                o1.patient == o2.patient
            }.min('startDate')
        }
        List result2 = query.list()
        log.info "RESULT: ${result2}"

        then:
        result2.size() == 0
    }

    void 'test CriteriaQueryBuilder with clinical data'() {
        setupData()

        FieldConstraint constraint = new FieldConstraint()
        constraint.field = patientAgeField
        constraint.operator = Operator.GREATER_THAN
        constraint.value = 60L
        ObservationQuery query = new ObservationQuery()
        query.queryType = QueryType.VALUES
        query.constraint = new Negation(arg: constraint)
        QueryBuilder builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )

        DetachedCriteria<ObservationFact> criteria = builder.build(query)

        when:
        def results = criteria.list()

        then:
        results.size() == 1
        (results[0] as ObservationFact).patient.id == testData.i2b2Data.patients[1].id
    }

    void 'test CriteriaQueryBuilder with observation value constraint'() {
        setupData()

        // Query for observations for values > 1
        ValueConstraint constraint = new ValueConstraint()
        constraint.valueType = Type.NUMERIC
        constraint.operator = Operator.GREATER_THAN
        constraint.value = 1L
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.VALUES,
                constraint: constraint
        )
        QueryBuilder builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )

        DetachedCriteria<ObservationFact> criteria = builder.build(query)

        when:
        def results = criteria.list()

        then:
        // should only return observations for patient testData.i2b2Data.patients[1]
        results.size() == 2
        results.each { it.valueType == ObservationFact.TYPE_NUMBER && it.numberValue > 1 }
    }

    void 'test CriteriaQueryBuilder with temporal constraints'() {
        setupData()

        org.transmartproject.db.i2b2data.ConceptDimension concept = org.transmartproject.db.i2b2data.ConceptDimension.find { conceptPath == '\\foo\\study1\\bar\\'}
        FieldConstraint constraint = new FieldConstraint()
        constraint.field = conceptCodeField
        constraint.operator = Operator.EQUALS
        constraint.value = concept.conceptCode
        ObservationQuery subquery = new ObservationQuery()
        subquery.queryType = QueryType.VALUES
        subquery.constraint = constraint
        QueryBuilder builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )

        DetachedCriteria<ObservationFact> criteria = builder.build(subquery)

        when:
        def results = criteria.list()
        log.info "Subquery results:"
        results.each {
            def json = ((ObservationFact)it) as JSON
            log.info json.toString(true)
        }

        then:
        results.size() == 1
        ((ObservationFact)results[0]).conceptCode == concept.conceptCode

        when:
        TemporalConstraint beforeConstraint = new TemporalConstraint(
                eventQuery: subquery,
                operator: Operator.BEFORE
        )
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.VALUES,
                constraint: beforeConstraint
        )
        DetachedCriteria<ObservationFact> temporalCriteria = builder.build(query)
        def temporalResults = temporalCriteria.list()
        log.info "Main query results:"
        temporalResults.each {
            def json = ((ObservationFact)it) as JSON
            log.info json.toString(true)
        }

        then:
        temporalResults instanceof List
        temporalResults.empty
    }

    void 'test CriteriaQueryBuilder with existential queries'() {
        setupData()

        org.transmartproject.db.i2b2data.ConceptDimension concept = org.transmartproject.db.i2b2data.ConceptDimension.find { conceptPath == '\\foo\\study1\\bar\\'}
        FieldConstraint constraint = new FieldConstraint()
        constraint.field = conceptCodeField
        constraint.operator = Operator.EQUALS
        constraint.value = concept.conceptCode
        ObservationQuery query = new ObservationQuery()
        query.queryType = QueryType.EXISTS
        query.constraint = constraint
        QueryBuilder builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )

        def criteria = builder.build(query)

        when:
        def result = criteria.asBoolean()
        log.info "Exists: ${result}"

        then:
        result

        when:
        query.constraint = new Negation(arg: constraint)
        builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )
        criteria = builder.build(query)
        result = criteria.asBoolean()
        log.info "Exists: ${result}"

        then:
        result

        when:
        constraint.value = "xyzabc"
        query.constraint = constraint
        builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )
        criteria = builder.build(query)
        result = criteria.asBoolean()
        log.info "Exists: ${result}"

        then:
        !result
    }

    void 'test CriteriaQueryBuilder with min and max queries'() {
        setupData()

        when:
        ObservationQuery query = new ObservationQuery(
                queryType: QueryType.MAX,
                select: ['numberValue'],
                constraint: new TrueConstraint()
        )
        QueryBuilder builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )
        def criteria = builder.build(query)
        def result = criteria.get()
        log.info "Max: ${result}"

        then:
        result == 100

        when:
        query.queryType = QueryType.MIN
        builder = new CriteriaQueryBuilder(
                studies: Study.findAll()
        )
        criteria = builder.build(query)
        result = criteria.get()
        log.info "Min: ${result}"

        then:
        result == 10
    }

}
