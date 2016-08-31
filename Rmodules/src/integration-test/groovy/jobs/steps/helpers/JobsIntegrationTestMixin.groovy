package jobs.steps.helpers

import grails.test.mixin.TestMixinTargetAware
import grails.util.Holders
import org.grails.test.support.GrailsTestInterceptor
import org.grails.test.support.GrailsTestMode
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.ApplicationContext

import static jobs.misc.AnalysisQuartzJobAdapter.cleanJobBeans

/*
 * based on IntegrationTestMixin, but I couldn't extend it because it would
 * screw up the weaving of the mixin
 */

class JobsIntegrationTestMixin implements TestMixinTargetAware {

    Object target
    GrailsTestInterceptor interceptor

    static ApplicationContext getCurrentApplicationContext() {
        Holders.applicationContext
    }

    void setTarget(Object target) {
        this.target = target
        try {
            if (currentApplicationContext && target) {

                interceptor = new GrailsTestInterceptor(target, new GrailsTestMode(
                        autowire: false, /* GrailsTestInterceptor's autowiring is too limited */
                        wrapInRequestEnvironment: true,
                        wrapInTransaction: target.hasProperty('transactional') ? target['transactional'] : true),
                        currentApplicationContext,
                        ['Spec', 'Specification', 'Test', 'Tests'] as String[])
            }
        } catch (IllegalStateException ise) {
            // ignore, thrown when application context hasn't been bootstrapped
        }
    }

    @Before
    void initIntegrationTest() {
        addJobNameBean()
        interceptor?.init()
        initializeTargetAsBean()
        if (target.respondsTo('before')) {
            target.before()
        }
    }

    @After
    void destroyIntegrationTest() {
        interceptor?.destroy()
    }

    @After
    void cleanupJobScope() {
        cleanJobBeans()
    }

    private void addJobNameBean() {
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues()
        constructorArgs.addIndexedArgumentValue(0, 'testJobName')

        currentApplicationContext.registerBeanDefinition(
                'jobName',
                new GenericBeanDefinition(
                        beanClass: String,
                        constructorArgumentValues: constructorArgs,
                        scope: 'job'))
    }

    private void initializeTargetAsBean() {
        initializeAsBean target
    }

    void initializeAsBean(Object object) {
        /*
         * The autowiring in GrailsTestInterceptor is too limited.
         * It only manually creates an AutowiredAnnotationBeanPostProcessor and
         * manually calls it (see GrailsTestAutowirer#autowire), so @Resource
         * annotations, which are handled by CommonAnnotationBeanPostProcessor,
         * are not ignored.
         */
        BeanFactory factory = currentApplicationContext.beanFactory
        /* "traditional" autowiring, also called in GrailsTestAutowirer#autowire */
        factory.autowireBeanProperties(object,
                AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        /* apply bean post processors' postProcessPropertyValues(), which will
         * do annotation injection courtesy of AutowiredAnnotationBeanPostProcessor
         * and CommonAnnotationBeanPostProcessor */
        factory.autowireBean object
    }
}
