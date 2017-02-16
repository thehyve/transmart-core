/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.util

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import org.grails.core.util.ClassPropertyFetcher
import org.grails.datastore.mapping.query.api.QueryableCriteria
import org.grails.orm.hibernate.query.HibernateQuery
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Subqueries
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.internal.CriteriaImpl

/**
 * Contains methods to overcome limitations in current GORM implementation.
 */
class GormWorkarounds {

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
}
