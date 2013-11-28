package org.transmartproject.db.dataquery.highdim.mrna

import grails.orm.HibernateCriteriaBuilder
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.i2b2data.PatientDimension

import static junit.framework.TestCase.fail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.hibernate.sql.JoinFragment.INNER_JOIN

class MrnaGeneDataConstraintTests {

    @Before
    void setUp() {
        MrnaTestData.saveAll()
    }

    private HibernateCriteriaBuilder createCriteriaBuilder() {
        HibernateCriteriaBuilder builder = DeSubjectMicroarrayDataCoreDb.createCriteria()
        builder.buildCriteria {
            createAlias('jProbe', 'p', INNER_JOIN)

            eq 'trialName', MrnaTestData.TRIAL_NAME
        }
        builder
    }

    @Test
    @Ignore
    void basicTestGene() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = MrnaGeneDataConstraint.createForLongIds([
                MrnaTestData.searchKeywords.
                        find({ it.keyword == 'BOGUSRQCD1' }).
                        uniqueId
                ])

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        assertThat res, allOf(
                hasSize(2),
                everyItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', equalTo('BOGUSRQCD1'))
                        )
                ),
                containsInAnyOrder(
                        hasProperty('patient', equalTo(MrnaTestData.patients[0])),
                        hasProperty('patient', equalTo(MrnaTestData.patients[1]))
                )
        )
    }

    @Test
    @Ignore
    void searchByKeywordIdTest() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* keywords for genes BOGUSCPO, BOGUSRQCD1 */
        def testee = MrnaGeneDataConstraint.
                createForSearchKeywordIds([ -501, -502 ])

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        assertThat res, allOf(
                hasSize(4),
                hasItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', equalTo('BOGUSCPO'))
                        )
                ),
                hasItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', equalTo('BOGUSRQCD1'))
                        )
                ),
        )
    }

    @Test
    @Ignore
    void basicTestGeneSignature() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* should map to BOGUSVNN3 directly and to BOGUSRQCD1 via bio_assay_data_annotation
         * See comment before MrnaTestData.searchKeywords */
        def testee = MrnaGeneDataConstraint.createForLongIds([
                MrnaTestData.searchKeywords.
                        find({ it.keyword == 'genesig_keyword_-602' }).
                        uniqueId
        ])

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        def matcherFor = { PatientDimension patient, String gene ->
            allOf(
                    hasProperty('probe',
                            hasProperty('geneSymbol', equalTo(gene))
                    ),
                    hasProperty('patient', equalTo(patient))
            )
        }

        assertThat res,
                /* 2 patients * 2 probes (one for each gene) */
                containsInAnyOrder(
                        matcherFor(MrnaTestData.patients[0], 'BOGUSRQCD1'),
                        matcherFor(MrnaTestData.patients[1], 'BOGUSRQCD1'),
                        matcherFor(MrnaTestData.patients[0], 'BOGUSVNN3'),
                        matcherFor(MrnaTestData.patients[1], 'BOGUSVNN3'),
                )
    }

    @Test
    @Ignore
    void testMixedConstraint() {
        try {
            MrnaGeneDataConstraint.createForLongIds(['GENE:234322', 'GENESIG:23434'])
            fail 'Expected exception'
        } catch (e) {
            assertThat e, isA(InvalidArgumentsException)
            assertThat e, hasProperty('message', containsString('exactly one type'))
        }
    }

    @Test
    @Ignore
    void testEmptyConstraint() {
        try {
            MrnaGeneDataConstraint.createForLongIds([])
            fail 'Expected exception'
        } catch (e) {
            assertThat e, isA(InvalidArgumentsException)
            assertThat e, hasProperty('message', containsString('exactly one type'))
        }
    }
}
