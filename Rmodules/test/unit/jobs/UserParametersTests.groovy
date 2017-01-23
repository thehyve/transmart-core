package jobs

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Test

/**
 * Created by carlos on 1/17/14.
 */
@TestMixin(GrailsUnitTestMixin)
class UserParametersTests {

    private Map<String,Object> myMap
    private UserParameters params

    @Before
    void setUp() {
        myMap = [foo:'bar ', number:123]
        params = new UserParameters(map: myMap)
    }

    @Test
    void testToJSON() {
        def expected = '''{"foo":"bar ","number":123}'''
        assert expected == params.toJSON()
    }
}
