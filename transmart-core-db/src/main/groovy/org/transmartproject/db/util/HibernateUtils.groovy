/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.util

import grails.orm.HibernateCriteriaBuilder
import org.grails.core.util.ClassPropertyFetcher
import org.hibernate.Criteria
import org.hibernate.StatelessSession
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Projection
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification
import org.hibernate.engine.spi.QueryParameters
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.engine.spi.SessionImplementor
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
            SessionImplementor session,
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
        impl.setSession((SessionImplementor) session);
        return impl;
    }

    /**
     * Workaround for bug https://github.com/grails/grails-core/issues/10403 in Grails 3.2.3 that the getters of
     * properties declared as e.g. cProtectedAccess are not correctly identified as such by the ClassPropertyFetcher.
     *
     * Properties like cProtectedAccess have a getter named getcProtectedAccess. Grails recognizes getters by
     * starting with 'get' and having the fourth letter capitalized, so it misses these. If this bug is fixed in
     * grails this workaround can be dropped.
     *
     * This workaround only fixes instance properties, not static properties.
     *
     * The problems from this Gorm bug manifested itself in that the loading of TestData in an integration test in
     * transmart-core-db-tests would result in a java.lang.IllegalArgumentException: "object is not an instance of
     * declaring class" at groovy.lang.MetaBeanProperty.getProperty(MetaBeanProperty.java:62), which is called
     * indirectly from the 'constraints' closure in domain classes which have affected properties.
     *
     * If loading of TestData works without this workaround, this can be removed.
     *
     * NB: not thread safe! But if called once from a class-to-fix's static initializer the chance of hitting a bad
     * race should be minimal.
     *
     * @param cls the class of which fix the property getter
     */
    static void fixupClassPropertyFetcher(Class cls) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(cls)
        Map<String, ClassPropertyFetcher.PropertyFetcher> instanceFetchers = cpf.instanceFetchers

        Set<String> toFix = instanceFetchers.keySet().findAll { String it ->
            it.length() >= 5 && it.startsWith("get") &&
                    Character.isLowerCase(it.charAt(3)) && Character.isUpperCase(it.charAt(4))
        }
        toFix.each {
            instanceFetchers[it.substring(3)] = instanceFetchers.remove(it)
        }
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
        SessionImplementor session = criteria.session
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
        SessionImplementor session = resultCriteria.session
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
        session.executeNativeUpdate(insertSpecification, sqlDetails.parameters)
    }
}
