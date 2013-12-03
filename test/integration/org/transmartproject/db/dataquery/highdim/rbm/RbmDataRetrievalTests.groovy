package org.transmartproject.db.dataquery.highdim.rbm

import com.google.common.collect.Lists
import groovy.sql.Sql
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

import javax.annotation.PostConstruct
import javax.sql.DataSource

import static groovy.test.GroovyAssert.shouldFail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.closeTo
import static org.hamcrest.Matchers.closeTo

class RbmDataRetrievalTests {

    RbmTestData testData = new RbmTestData()

    HighDimensionResource highDimensionResourceService

    HighDimensionDataTypeResource rbmResource

    AssayConstraint trialNameConstraint

    Projection projection

    TabularResult result

    @Autowired
    @Qualifier('dataSource')
    DataSource dataSource

    private Sql sql

    @PostConstruct
    void init() {
        this.sql = new Sql(dataSource.connection)
    }

    double delta = 0.0001

    @Test
    void testRetrievalByTrialNameAssayConstraint() {
        result = rbmResource.retrieveData([ trialNameConstraint ], [], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                hasSize(3),
                everyItem(
                        hasProperty('data',
                                allOf(
                                        hasSize(2),
                                        everyItem(isA(Double))
                                )
                        )
                ),
                contains(
                        hasProperty('data', contains(
                                closeTo(testData.rbmData[-1].zscore as Double, delta),
                                closeTo(testData.rbmData[-2].zscore as Double, delta),
                        )),
                        hasProperty('data', contains(
                                closeTo(testData.rbmData[-3].zscore as Double, delta),
                                closeTo(testData.rbmData[-4].zscore as Double, delta),
                        )),
                        hasProperty('data', contains(
                                closeTo(testData.rbmData[-5].zscore as Double, delta),
                                closeTo(testData.rbmData[-6].zscore as Double, delta),
                        )),
                )
        )
    }

    @Test
    void testRetrievalByUniProtNamesDataConstraint() {
        def proteinDataConstraint = rbmResource.createDataConstraint(
                [names: [ 'Adiponectin' ]],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        result = rbmResource.retrieveData([ trialNameConstraint ], [ proteinDataConstraint ], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(
                        hasProperty('data', allOf(
                                hasSize(2),
                                contains(
                                        closeTo(testData.rbmData[-5].zscore as Double, delta),
                                        closeTo(testData.rbmData[-6].zscore as Double, delta),
                                ))
                        )
                ),
                contains(hasProperty('uniprotId', equalTo('Q15848')))
        )
    }

    @Test
    void testRetrievalByGeneNamesDataConstraint() {
        def geneDataConstraint = rbmResource.createDataConstraint(
                [names: [ 'SLC14A2' ]],
                DataConstraint.GENES_CONSTRAINT
        )

        result = rbmResource.retrieveData([ trialNameConstraint ], [ geneDataConstraint ], projection)

        def resultList = Lists.newArrayList result

        assertThat resultList, allOf(
                hasSize(1),
                everyItem(
                        hasProperty('data', allOf(
                                hasSize(2),
                                contains(
                                        closeTo(testData.rbmData[-3].zscore as Double, delta),
                                        closeTo(testData.rbmData[-4].zscore as Double, delta),
                                ))
                        )
                ),
                contains(hasProperty('uniprotId', equalTo('Q15849')))
        )
    }

    @Before
    void setUp() {
        testData.saveAll()
        rbmResource = highDimensionResourceService.getSubResourceForType 'rbm'

        trialNameConstraint = rbmResource.createAssayConstraint(
                AssayConstraint.TRIAL_NAME_CONSTRAINT,
                name: RbmTestData.TRIAL_NAME,
        )
        projection = rbmResource.createProjection [:], 'default_real_projection'
    }

    @After
    void tearDown() {
        result?.close()
    }

}
