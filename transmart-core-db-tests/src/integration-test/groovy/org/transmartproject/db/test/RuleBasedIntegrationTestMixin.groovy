/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.test

import grails.test.mixin.TestMixinTargetAware
import grails.test.mixin.integration.IntegrationTestMixin
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j
import org.grails.test.support.GrailsTestInterceptor
import org.grails.test.support.GrailsTestMode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule

/**
 * Grails weaves into every integration test class {@link IntegrationTestMixin},
 * which has a big problem for us: it includes a methods annotated with
 * {@link Before} and {@link After}.
 *
 * Thus, if we have methods in the test class also annotated with
 * {@link Before} and {@link After}, we won't know the order these will be run
 * relatively to those provided by the mixin. Because the mixin methods to
 * things that are relevant for our tests methods, like setting up a
 * transaction, they must be before (in case of the {@link Before} method)
 * or after (ion case of the {@link After}) our test methods annotated with
 * the same classes.
 *
 * This alternative mixin provides the same behavior but it relies on junit
 * rules instead of methods annotated with {@link Before} and {@link After},
 * and hence the initialization/tear down of the test is always done in the
 * correct order (in a shell outside the test class).
 *
 * Use it like this:
 *
 * <pre>
 *     @TestMixin(RuleBasedIntegrationTestMixin)
 *     class FooBarTests { ... }
 * </pre>
 */
class RuleBasedIntegrationTestMixin implements TestMixinTargetAware {

    Object target
    GrailsTestInterceptor interceptor

    @CompileStatic(TypeCheckingMode.SKIP)
    void setTarget(Object target) {
        this.target = target
        try {
            final applicationContext = IntegrationTestPhaseConfigurer.currentApplicationContext
            if(applicationContext && target) {
                interceptor = new GrailsTestInterceptor(target, new GrailsTestMode( autowire: true,
                        wrapInRequestEnvironment: true,
                        wrapInTransaction: target.hasProperty('transactional') ? target['transactional'] : true),
                        applicationContext,
                        ['Spec', 'Specification','Test', 'Tests'] as String[] )
            }
        } catch (IllegalStateException ise) {
            // ignore, thrown when application context hasn't been bootstrapped
        }
    }

    @Rule
    TestRule getGrailsInterceptorRule() {
        // has to be a method otherwise TestMixinTransformation won't weave this in
        new GrailsInterceptorRule(interceptor: interceptor,
                                  target: target)
    }

    void autowireByType() {
        /* "traditional: (i.e. not postprocessor/annotation based)
         * autowiring by type */
        IntegrationTestPhaseConfigurer.currentApplicationContext.
                autowireCapableBeanFactory.autowireBeanProperties(target,
                AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE,
                false)
    }

    @Log4j
    static class GrailsInterceptorRule extends ExternalResource {
        GrailsTestInterceptor interceptor
        Object target

        @Override
        protected void before() throws Throwable {
            interceptor.init()
            neuterIntegrationTestMixin()
        }

        void neuterIntegrationTestMixin() {
            /* Grails gets in our way here...
             *
             * GrailsIntegrationTestCompiler always applies IntegrationTestMixinTransformation,
             * which weaves in IntegrationTestMixin via TestMixinTransformation.
             * The end result is the same as if every integration test had @TestMixin(IntegrationTestMixin)
             * This happens even if the test class is annotation with @TestMixin(SomethingElseMixin),
             * Because we don't want it being used, we have to neuter the
             * @After/@Before methods the mixin adds.
             * We do that by setting the interceptor to null */
            // TODO: this fails if the test has a superclass?
            target.$integrationTestMixin.interceptor = null
        }

        @Override
        protected void after() {
            interceptor.destroy()
        }
    }

}
