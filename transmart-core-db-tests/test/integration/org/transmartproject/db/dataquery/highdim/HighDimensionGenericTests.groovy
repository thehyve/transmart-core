package org.transmartproject.db.dataquery.highdim

import grails.util.Holders
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.metabolite.MetaboliteTestData
import org.transmartproject.db.dataquery.highdim.mirna.MirnaTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.dataquery.highdim.protein.ProteinTestData
import org.transmartproject.db.dataquery.highdim.rbm.RbmTestData
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqTestData
import org.transmartproject.db.dataquery.highdim.rnaseqcog.RnaSeqCogTestData

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by jan on 2/4/14.
 */

/*
 * This should be done with parametrized tests, but that doesn't work in grails 2.2, it needs 2.3.
 * For now this test is made abstract and extended for the specific HD data types.
 *
 * FIXME when the grails version is updated
 */
//@RunWith(value = Parameterized)
abstract class HighDimensionGenericTests {

    HighDimensionDataTypeResource type
    List<String> dataProperties
    List<String> rowProperties
    def testData

    HighDimensionGenericTests(HighDimensionDataTypeResource type, List<String> dataProperties, List<String> rowProperties, Class testData) {
        this.type = type
        this.dataProperties = dataProperties
        this.rowProperties = rowProperties
        this.testData = testData.newInstance()
    }

    /**
     * A convenience constructor for the workaround for parameterized tests
     */
    HighDimensionGenericTests(String typeName, List<String> dataProperties, List<String> rowProperties, Class testData) {
        HighDimensionResource hdrs = Holders.grailsApplication.mainContext.getBean('highDimensionResourceService')
        this.type = hdrs.getSubResourceForType(typeName)
        this.dataProperties = dataProperties
        this.rowProperties = rowProperties
        this.testData = testData.newInstance()
    }

    @Before
    void setUp() {
        assertThat type, is(notNullValue())
        assertThat testData, is(notNullValue())
        testData.saveAll()
    }

    @Test
    void testDescription() {
        assertThat type.dataTypeDescription, instanceOf(String)
    }

    @Test
    void testAllDataProjection() {
        AllDataProjection genericProjection = type.createProjection(Projection.ALL_DATA_PROJECTION)

        def result = type.retrieveData([], [], genericProjection)
        def indicesList = result.indicesList
        def firstrow = result.iterator().next()

        assertThat firstrow, is(notNullValue())
        rowProperties.each {
            assertThat genericProjection.rowProperties, hasItem(it)
        }
        genericProjection.rowProperties.each {
            assertThat firstrow, hasProperty(it)
        }

        def data = firstrow[indicesList[0]]

        assertThat data, is(notNullValue())
        assertThat data, is(instanceOf(Map))
        dataProperties.each {
            assertThat genericProjection.dataProperties, hasItem(it)
        }
        genericProjection.dataProperties.each {
            assertThat data, hasKey(it)
        }
    }

}
