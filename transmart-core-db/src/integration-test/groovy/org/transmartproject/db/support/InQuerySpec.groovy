package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.db.i2b2data.PatientDimension
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Supplier

@Rollback
@Integration
class InQuerySpec extends Specification {

    @Unroll
    void 'test postgres query splitting for list of #values.size() values'(Integer expectedCount, List<Long> values) {
        when: 'Querying for patients'
        Supplier<HibernateCriteriaBuilder> criteriaBuilderProducer = { ->
            PatientDimension.createCriteria() as HibernateCriteriaBuilder
        }
        List result = InQuery.listIn(criteriaBuilderProducer, 'id', values)

        then: 'The expected amount of objects is returned'
        result.size() == expectedCount

        where: 'sizes range from 0 to more than 30000'
        expectedCount | values
        0             | []
        1             | [-40L]
        3             | [-41L, -42L, -43L]
        1250          | (-20000L .. 20000L)
    }

}
