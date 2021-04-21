/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.util

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.Criteria
import org.hibernate.StatelessSession
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projection
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification
import org.hibernate.engine.spi.QueryParameters
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.internal.CriteriaImpl
import org.hibernate.loader.criteria.CriteriaJoinWalker
import org.hibernate.loader.criteria.CriteriaQueryTranslator
import org.hibernate.persister.entity.OuterJoinLoadable
import org.hibernate.type.Type

/**
 * Contains methods to overcome limitations in current GORM implementation.
 */
class HibernateUtils {

    /*
     * Workaround to force the usage of a specific session, e.g. to use a stateless session instead of the default
     */

    final static HibernateCriteriaBuilder createCriteriaBuilder(
            Class targetClass,
            String alias,
            SharedSessionContractImplementor session,
            readOnly = true,
            cacheable = false,
            fetchSize = 10000) {

        HibernateCriteriaBuilder builder = new HibernateCriteriaBuilder(targetClass, session.factory)

        /* we have to write a private here */
        if (session) {
            //force usage of a specific session (probably stateless)
            builder.criteria = new CriteriaImpl(targetClass.canonicalName,
                    alias,
                    session)
            builder.criteriaMetaClass = GroovySystem.metaClassRegistry.
                    getMetaClass(builder.criteria.getClass())
        } else {
            builder.createCriteriaInstance()
        }

        /* builder.instance.is(builder.criteria) */
        builder.instance.readOnly = readOnly
        builder.instance.cacheable = cacheable
        builder.instance.fetchSize = fetchSize

        builder
    }

    /*
     * A DetachedCriteria.getExecutableCriteria that accepts a StatelessSession.
     * This method was not implemented by Hibernate itself because the Criteria api is deprecated. (Too bad Grails is
     * still based on it.) See https://hibernate.atlassian.net/browse/HHH-2625.
     */

    final static Criteria getExecutableCriteria(DetachedCriteria detachedCriteria, StatelessSession session) {
        CriteriaImpl impl = detachedCriteria.criteriaImpl
        impl.setSession((SharedSessionContractImplementor) session);
        return impl;
    }

    static class NativeSQLQueryDetails {
        final NativeSQLQuerySpecification specification
        final QueryParameters parameters

        private NativeSQLQueryDetails(NativeSQLQuerySpecification specification, QueryParameters parameters) {
            this.specification = specification
            this.parameters = parameters
        }
    }

    static NativeSQLQueryDetails getNativeSQLQueryDetails(Criteria criteria) {
        assert criteria instanceof CriteriaImpl
        SharedSessionContractImplementor session = criteria.session
        SessionFactoryImplementor factory = session.factory
        CriteriaQueryTranslator translator = new CriteriaQueryTranslator(factory, criteria, criteria.entityOrClassName,
                CriteriaQueryTranslator.ROOT_SQL_ALIAS)
        String[] implementors = factory.getImplementors(criteria.entityOrClassName)
        String implementor = implementors[0]
        OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) factory.getEntityPersister(implementor)
        CriteriaJoinWalker walker = new CriteriaJoinWalker(outerJoinLoadable,
                translator,
                factory,
                criteria,
                criteria.entityOrClassName,
                session.loadQueryInfluencers)

        String sql = walker.SQLString
        Projection projection = criteria.projection
        List<Type> types = projection.getTypes(criteria, translator) as List<Type>
        NativeSQLQueryReturn[] queryReturns = types.withIndex()
                .collect { Type type, Integer index ->
            String columnAlias = projection.getColumnAliases(index)[0]
            new NativeSQLQueryScalarReturn(columnAlias, type)
        } as NativeSQLQueryScalarReturn[]
        Set querySpaces = walker.querySpaces
        QueryParameters queryParameters = translator.queryParameters

        new NativeSQLQueryDetails(
                new NativeSQLQuerySpecification(sql, queryReturns, querySpaces),
                queryParameters
        )
    }

    static int insertResultToTable(final Class entityClass, final List<String> properties, final Criteria resultCriteria) {
        assert resultCriteria instanceof CriteriaImpl
        SharedSessionContractImplementor session = resultCriteria.session
        SessionFactoryImplementor factory = session.factory
        String[] implementors = factory.getImplementors(entityClass.name)
        String implementor = implementors[0]
        OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) factory.getEntityPersister(implementor)
        String tableName = outerJoinLoadable.tableName
        List<String> columns = properties
                .collect { String column -> outerJoinLoadable.getPropertyColumnNames(column) as List }.flatten()

        def sqlDetails = getNativeSQLQueryDetails(resultCriteria)
        String csColumns = columns.join(', ')
        if (csColumns) {
            csColumns = "(${csColumns})"
        }
        NativeSQLQuerySpecification specification = sqlDetails.specification
        NativeSQLQuerySpecification insertSpecification = new NativeSQLQuerySpecification(
                "insert into ${tableName}${csColumns} ${specification.queryString}",
                specification.queryReturns,
                specification.querySpaces
        )
        int updateCount = session.executeNativeUpdate(insertSpecification, sqlDetails.parameters)
        session.connection().commit()
        updateCount
    }
}
