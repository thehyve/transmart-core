package jobs.steps.helpers

import grails.test.mixin.TestMixin
import groovy.transform.EqualsAndHashCode
import org.gmock.GMockController
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.core.dataquery.clinical.ClinicalDataResource
import org.transmartproject.core.dataquery.clinical.ClinicalVariable

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.sameInstance

@TestMixin(JobsIntegrationTestMixin)
class ClinicalDataRetrieverTests {

    @Delegate(interfaces = false)
    @Autowired
    GMockController gMockController

    public static final String CONCEPT_PATH_1 = '\\\\foo\\bar\\1\\'

    @Autowired
    ClinicalDataResource clinicalDataResource

    @Autowired
    ClinicalDataRetriever testee

    @Before
    void before() {

    }

    @EqualsAndHashCode(includes = 'conceptPath')
    static class MockClinicalVariable implements ClinicalVariable {
        String conceptPath

        @Override
        ConceptKey getKey() {
            return null
        }
    }

    void testRepeatedClinicalVariable() {
        def mockVariable = new MockClinicalVariable(conceptPath: CONCEPT_PATH_1)
        clinicalDataResource.createClinicalVariable(
                concept_path: CONCEPT_PATH_1,
                ClinicalVariable.TERMINAL_CONCEPT_VARIABLE).
                returns(mockVariable).times(2)

        /* when we add a variable equal to a previous one, then the
         * state of the ClinicalDataRetriever is not changed and
         * << returns the previous variable that was added.
         * This should be used instead of the submitted one in
         * interactions with query result */

        play {
            assertThat testee << CONCEPT_PATH_1, sameInstance(mockVariable)
            assertThat testee << CONCEPT_PATH_1, sameInstance(mockVariable)
        }
        assertThat testee << mockVariable, sameInstance(mockVariable)

        def equalClinicalVariable =
                new MockClinicalVariable(conceptPath: CONCEPT_PATH_1)

        assertThat testee << equalClinicalVariable, sameInstance(mockVariable)

        assertThat testee.variables, contains(mockVariable)
    }

}
