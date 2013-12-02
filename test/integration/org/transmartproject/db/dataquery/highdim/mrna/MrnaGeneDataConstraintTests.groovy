package org.transmartproject.db.dataquery.highdim.mrna

import grails.orm.HibernateCriteriaBuilder
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.i2b2data.PatientDimension

import static junit.framework.TestCase.fail
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.hibernate.sql.JoinFragment.INNER_JOIN

class MrnaGeneDataConstraintTests {

    MrnaModule mrnaModule

    MrnaTestData testData = new MrnaTestData()

    @Before
    void setUp() {
        testData.saveAll()
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
    void basicTestGene() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = mrnaModule.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                [ keyword_ids: testData.searchKeywords.
                        findAll({ it.keyword == 'BOGUSRQCD1' })*.id ])

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
                        hasProperty('patient', equalTo(testData.patients[0])),
                        hasProperty('patient', equalTo(testData.patients[1]))
                )
        )
    }

    @Test
    void testWithMultipleGenes() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* keywords ids for genes BOGUSCPO, BOGUSRQCD1 */
        def testee = mrnaModule.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                [ keyword_ids: [ '-501', '-502' ] ])


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

    private static Matcher matcherFor(PatientDimension patient, String gene) {
        allOf(
                hasProperty('probe',
                        hasProperty('geneSymbol', equalTo(gene))
                ),
                hasProperty('patient', equalTo(patient))
        )
    }

    @Test
    void basicTestGeneSignature() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* should map to BOGUSVNN3 directly and to BOGUSRQCD1 via bio_assay_data_annotation
         * See comment before MrnaTestData.searchKeywords */
        def testee = mrnaModule.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                [ keyword_ids: testData.searchKeywords.
                        findAll({ it.keyword == 'genesig_keyword_-602' })*.id ])

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        assertThat res,
                /* 2 patients * 2 probes (one for each gene) */
                containsInAnyOrder(
                        matcherFor(testData.patients[0], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[1], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[0], 'BOGUSVNN3'),
                        matcherFor(testData.patients[1], 'BOGUSVNN3'),
                )
    }

    @Test
    void testMixedConstraint() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* should map to BOGUSVNN3 directly and to BOGUSRQCD1 via bio_assay_data_annotation
         * See comment before MrnaTestData.searchKeywords */
        def testee = mrnaModule.createDataConstraint(
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                [ keyword_ids: testData.searchKeywords.
                        findAll({
                            it.keyword == 'genesig_keyword_-602' ||
                                    it.keyword == 'BOGUSCPO'
                        })*.id ])

        assertThat testee, is(instanceOf(DisjunctionDataConstraint))

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        assertThat res,
                /* 2 patients * 3 probes (one for each gene) */
                containsInAnyOrder(
                        matcherFor(testData.patients[0], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[1], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[0], 'BOGUSVNN3'),
                        matcherFor(testData.patients[1], 'BOGUSVNN3'),
                        matcherFor(testData.patients[0], 'BOGUSCPO'),
                        matcherFor(testData.patients[1], 'BOGUSCPO'),
                )
    }

    @Test
    @Ignore
    void testEmptyConstraint() {
        try {
            mrnaModule.createDataConstraint(
                    DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                    [ keyword_ids: [] ])
            fail 'Expected exception'
        } catch (e) {
            assertThat e, isA(InvalidArgumentsException)
            assertThat e, hasProperty('message', containsString('exactly one type'))
        }
    }
}
