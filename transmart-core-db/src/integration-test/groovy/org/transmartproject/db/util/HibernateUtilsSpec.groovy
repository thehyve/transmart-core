/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.util

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.hibernate.Criteria
import org.hibernate.SessionFactory
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn
import org.hibernate.type.LongType
import org.hibernate.type.StandardBasicTypes
import org.hibernate.type.StringType
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.querytool.QueryResultType
import org.transmartproject.core.querytool.QueryStatus
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.querytool.*
import spock.lang.Specification

@Rollback
@Integration
class HibernateUtilsSpec extends Specification {

    @Autowired
    SessionFactory sessionFactory

    def 'get sql details out of criteria'() {
        def criteria = sessionFactory.currentSession.createCriteria(ObservationFact, 'obs')
        criteria.setProjection(Projections.alias(Projections.property('patient.id'), 'p'))
        criteria.add(Restrictions.eq('conceptCode', 'test'))

        when:
        HibernateUtils.NativeSQLQueryDetails sqlQueryDetails = HibernateUtils.getNativeSQLQueryDetails(criteria)
        then:
        sqlQueryDetails
        sqlQueryDetails.specification
        sqlQueryDetails.specification.queryString
        sqlQueryDetails.specification.queryString.toLowerCase().contains('patient_num as')
        sqlQueryDetails.specification.queryString.toLowerCase().contains(' from i2b2demodata.observation_fact ')
        sqlQueryDetails.specification.queryReturns
        sqlQueryDetails.specification.queryReturns.length == 1
        sqlQueryDetails.specification.queryReturns[0].class == NativeSQLQueryScalarReturn
        ((NativeSQLQueryScalarReturn) sqlQueryDetails.specification.queryReturns[0]).type
        ((NativeSQLQueryScalarReturn) sqlQueryDetails.specification.queryReturns[0]).type.class == LongType
        sqlQueryDetails.parameters
        sqlQueryDetails.parameters.positionalParameterTypes
        sqlQueryDetails.parameters.positionalParameterTypes.length == 1
        sqlQueryDetails.parameters.positionalParameterTypes[0].class == StringType
        sqlQueryDetails.parameters.positionalParameterValues
        sqlQueryDetails.parameters.positionalParameterValues.length == 1
        sqlQueryDetails.parameters.positionalParameterValues[0] == 'test'
    }

    def 'insert results of criteria to a table'() {
        QtQueryMaster queryMaster = new QtQueryMaster(
                name: 'test-fake-query-1',
                userId: 'fake-user',
                groupId: 'fake group',
                createDate: new Date(),
                requestXml: ''
        )

        QtQueryInstance queryInstance = new QtQueryInstance(
                userId: 'fake-user',
                groupId: 'fake group',
                startDate: new Date(),
                statusTypeId: QueryStatus.COMPLETED.id,
                queryMaster: queryMaster,
        )
        queryMaster.addToQueryInstances(queryInstance)

        QtQueryResultInstance resultInstance = new QtQueryResultInstance(
                statusTypeId: QueryStatus.FINISHED.id,
                startDate: new Date(),
                queryInstance: queryInstance,
                setSize: -1,
                realSetSize: -1,
                queryResultType: QtQueryResultType.load(QueryResultType.PATIENT_SET_ID),
        )
        queryInstance.addToQueryResults(resultInstance)
        queryMaster.save(flush: true)

        Criteria patientCriteria =
                sessionFactory.currentSession.createCriteria(ObservationFact, 'obs')
                        .setProjection(
                        Projections.projectionList()
                                .add(Projections.distinct(Projections.property('patient.id')), 'p')
                                .add(Projections.sqlProjection("${resultInstance.id} as rid", ['rid'] as String[],
                                    [StandardBasicTypes.LONG] as Type[])))

        Integer beforePatientSetsCount = QtPatientSetCollection.count()
        Integer selectedPatientsNumber = patientCriteria.list().size()

        when:
        def recordsInserted = HibernateUtils
                .insertResultToTable(QtPatientSetCollection, ['patient.id', 'resultInstance.id'], patientCriteria)
        Integer afterPatientSetsCount = QtPatientSetCollection.count()
        then:
        recordsInserted == selectedPatientsNumber
        afterPatientSetsCount == beforePatientSetsCount + selectedPatientsNumber
    }
}
