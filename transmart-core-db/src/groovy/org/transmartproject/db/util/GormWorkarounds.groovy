package org.transmartproject.db.util

import grails.orm.HibernateCriteriaBuilder
import org.codehaus.groovy.grails.orm.hibernate.query.HibernateQuery
import org.grails.datastore.mapping.query.api.QueryableCriteria
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Subqueries
import org.hibernate.engine.SessionImplementor
import org.hibernate.impl.CriteriaImpl

/**
 * Contains methods to overcome limitations in current GORM implementation.
 */
class GormWorkarounds {

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

    final static Criterion getHibernateInCriterion(
            String property,
            QueryableCriteria<?> queryableCriteria) {

        def hibDetachedCriteria = getHibernateDetachedCriteria(queryableCriteria)
        Subqueries.propertyIn(property, hibDetachedCriteria)
    }

    final static org.hibernate.criterion.DetachedCriteria getHibernateDetachedCriteria(
            QueryableCriteria<?> queryableCriteria) {

        String alias = queryableCriteria.getAlias()
        def persistentEntity = queryableCriteria.persistentEntity
        Class targetClass = persistentEntity.javaClass
        org.hibernate.criterion.DetachedCriteria detachedCriteria

        if(alias != null) {
            detachedCriteria = org.hibernate.criterion.DetachedCriteria.forClass(targetClass, alias)
        }
        else {
            detachedCriteria = org.hibernate.criterion.DetachedCriteria.forClass(targetClass)
        }
        def hq = new HibernateQuery(detachedCriteria)
        //To escape NPE we have to set this private field
        //This fix is the main reason to have this method @{see HibernateCriteriaBuilder.getHibernateDetachedCriteria}
        hq.entity = persistentEntity
        HibernateCriteriaBuilder.populateHibernateDetachedCriteria(
                hq,
                detachedCriteria,
                queryableCriteria)

        detachedCriteria;
    }
}
