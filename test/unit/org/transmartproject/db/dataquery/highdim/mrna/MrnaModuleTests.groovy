package org.transmartproject.db.dataquery.highdim.mrna

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.gmock.GMockTestCase
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.sameInstance


@TestMixin(GrailsUnitTestMixin)
class MrnaModuleTests extends GMockTestCase {

    MrnaModule testee = new MrnaModule()

    @Test
    void testSearchKeywordConstraintGeneration() {

        List keywordIdsInput = [ 10, 11.0, '0012' ]
        List typedKeywordIds = [ 10L, 11L, 12L ]

        def mockMrnaGeneDataConstraint = mock(MrnaGeneDataConstraint)
        def mockDataConstraint = mock(DataConstraint)

        mockMrnaGeneDataConstraint.static.
                createForSearchKeywordIds(typedKeywordIds).
                returns(mockDataConstraint)

        play {
            def factories = testee.createDataConstraintFactories()
            def result = factories.findResult {
                DataRetrievalParameterFactory factory ->

                factory.createFromParameters(
                        DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                        [ keyword_ids: keywordIdsInput ],
                        factory.&createFromParameters)
            }

            assertThat result, is(sameInstance(mockDataConstraint))
        }
    }


}
