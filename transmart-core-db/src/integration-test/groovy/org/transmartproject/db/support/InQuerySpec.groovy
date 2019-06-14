package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.i2b2data.PatientMapping
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Supplier

@Rollback
@Integration
class InQuerySpec extends Specification {

    @Unroll
    void 'test listIn fetches requested entities'(Integer expectedCount, List<Long> values) {
        when: 'Querying for patient mappings'
        Supplier<HibernateCriteriaBuilder> criteriaBuilderProducer = { ->
            PatientMapping.createCriteria() as HibernateCriteriaBuilder
        }
        List result = InQuery.listIn(criteriaBuilderProducer, 'encryptedId', values)

        then: 'The expected amount of patient mappings is returned'
        result.size() == expectedCount

        where:
        expectedCount | values
        0             | []
        1             | ['CV:40']
        3             | ['CT:41', 'EHR:42', 'TNS:43']
    }

    void 'test listIn gets actual patients by wide range of ids'() {
        when: 'Querying for patients'
        Supplier<HibernateCriteriaBuilder> criteriaBuilderProducer = { ->
            PatientDimension.createCriteria() as HibernateCriteriaBuilder
        }
        List result = InQuery.listIn(criteriaBuilderProducer, 'id', (-20000L..20000L))

        then: 'The actual amount of patient is returned'
        result.size() == PatientDimension.count()
    }

}
