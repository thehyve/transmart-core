import groovy.util.logging.Log4j
import org.codehaus.groovy.runtime.ConvertedMap
import org.codehaus.groovy.runtime.InvokerHelper
import org.gmock.GMockController
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.querytool.QueriesResource

@Log4j
class GMockFactoryBean implements FactoryBean {

    final boolean singleton = true

    Class clazz

    @Autowired
    GMockController gMockController

    private Object object

    def getObject() {
        if (clazz == null) {
            /* refuse to give an object if not fully initialized yet */
            throw new BeanCreationException('Asked to give mock without class being specified')
        }
        if (object == null) {
            object = gMockController.mock(clazz)
        }
        object
    }

    @Override
    Class<?> getObjectType() { clazz }
}

/* can't use ScopedProxyFactoryBean because it creates a JDK proxy,
 * which checks whether the returned value matches the value declared
 * on the interface method.
 */
class GroovyInterceptableProxyFactoryBean implements FactoryBean {

    Class<?> objectType
    String targetBeanName

    @Autowired
    ApplicationContext ctx
    final boolean singleton = true

    @Lazy Object object = {
        assert targetBeanName != null
        assert objectType != null

        java.lang.reflect.Proxy.newProxyInstance(
                objectType.classLoader,
                [objectType, GroovyInterceptable] as Class<?>[],
                new ConvertedMap(invokeMethod: { String name, Object args ->
                    InvokerHelper.invokeMethodSafe(
                            ctx.getBean(targetBeanName),
                            name,
                            args)
                }))
    }()
}

beans = {
    /*
     *  Note that this file is only used for testing.
     *  It is excluded from the final plugin artifact
     */

    gmockController GMockController, { bean ->
        bean.scope = 'job'
    }

    /* Mock the core-db services */

    highDimensionResourceService GroovyInterceptableProxyFactoryBean, {
        targetBeanName = 'scopedHighDimensionResourceService'
        objectType = HighDimensionResource
    }

    clinicalDataResourceService GroovyInterceptableProxyFactoryBean, {
        targetBeanName = 'scopedClinicalDataResourceService'
        objectType = ClinicalDataResource
    }

    queriesResourceService GroovyInterceptableProxyFactoryBean, {
        targetBeanName = 'scopedQueriesResourceService'
        objectType = QueriesResource
    }

    scopedHighDimensionResourceService GMockFactoryBean, { bean ->
        clazz = HighDimensionResource
        bean.scope = 'job'
    }

    scopedClinicalDataResourceService GMockFactoryBean, { bean ->
        clazz = ClinicalDataResource
        bean.scope = 'job'
    }

    scopedQueriesResourceService GMockFactoryBean, { bean ->
        clazz = QueriesResource
        bean.scope = 'job'
    }

}
