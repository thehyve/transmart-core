package jobs.steps.helpers

import grails.test.mixin.TestMixinTargetAware
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import org.codehaus.groovy.grails.test.support.GrailsTestInterceptor
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.junit.After
import org.junit.Before
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.GenericBeanDefinition

import static jobs.misc.AnalysisQuartzJobAdapter.cleanJobBeans
import static org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer.currentApplicationContext

/*
 * based on IntegrationTestMixin, but I couldn't extend it because it would
 * screw up the weaving of the mixin
 */
class JobsIntegrationTestMixin implements TestMixinTargetAware {

    Object target
    GrailsTestInterceptor interceptor

    @Override
    void setTarget(Object target) {
        this.target = target
        try {
            final applicationContext = IntegrationTestPhaseConfigurer.currentApplicationContext
            if (applicationContext && target) {

                interceptor = new GrailsTestInterceptor(target, new GrailsTestMode(
                        autowire: false, /* GrailsTestInterceptor's autowiring is too limited */
                        wrapInRequestEnvironment: true,
                        wrapInTransaction: target.hasProperty('transactional') ? target['transactional'] : true),
                        applicationContext,
                        ['Spec', 'Specification','Test', 'Tests'] as String[] )
            }
        } catch (IllegalStateException ise) {
            // ignore, thrown when application context hasn't been bootstrapped
        }
    }

    @Override
    @Before
    void initIntegrationTest() {
        //addTestJobsParamsBean()
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

    private void addTestJobsParamsBean() {
        currentApplicationContext.registerBeanDefinition(
                'jobParameters',
                new GenericBeanDefinition(
                        beanClass:     TestJobParamsBean,
                        scope:         'job',
                        propertyValues: new MutablePropertyValues(map: [:])))
    }

    private void addJobNameBean() {
        ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues()
        constructorArgs.addIndexedArgumentValue(0, 'testJobName')

        currentApplicationContext.registerBeanDefinition(
                'jobName',
                new GenericBeanDefinition(
                        beanClass:                 String,
                        constructorArgumentValues: constructorArgs,
                        scope:                     'job'))
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
