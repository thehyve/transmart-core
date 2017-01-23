/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.mrna

import grails.orm.HibernateCriteriaBuilder
import org.hamcrest.Matcher
import org.junit.Before
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

        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.
                findAll({ it.keyword == 'BOGUSRQCD1' })*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

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
        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.findAll {
            ['BOGUSCPO', 'BOGUSRQCD1'].contains(it.keyword)
        }*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )


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
        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.
                findAll({ it.keyword == 'bogus_gene_sig_-602' })*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

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
        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.
                findAll({
                    it.keyword == 'bogus_gene_sig_-602' ||
                            it.keyword == 'BOGUSCPO'
                })*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

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
    void testProteinConstraintByExternalId() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* there's a correlation between the
         * protein BOGUSCBPO_HUMAN and the gene BOGUSCPO */

        def testee = mrnaModule.createDataConstraint([ids: testData.bioMarkerTestData.proteinBioMarkers.
                find { it.name == 'BOGUSCBPO_HUMAN' }*.externalId],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        assertThat res, everyItem(
                hasProperty('probe',
                        hasProperty('geneSymbol', equalTo('BOGUSCPO'))))
    }

    @Test
    void testProteinConstraintByName() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = mrnaModule.createDataConstraint([names: ['BOGUSCBPO_HUMAN']],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        assertThat res, allOf(
                hasSize(2), /* 1 probe * 2 patients */
                everyItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', equalTo('BOGUSCPO')))))
    }

    @Test
    void testGeneSignatureConstraintByName() {
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = mrnaModule.createDataConstraint([names: ['bogus_gene_sig_-602']],
                DataConstraint.GENE_SIGNATURES_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        assertThat builder.instance.list(), allOf(
                hasSize(4), /* 2 probes * 2 patients */
                everyItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', anyOf(
                                        equalTo('BOGUSVNN3'),
                                        equalTo('BOGUSRQCD1'))))))
    }

    @Test
    void testEmptyConstraint() {
        try {
            mrnaModule.createDataConstraint([keyword_ids: []],
                    DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
            )
            fail 'Expected exception'
        } catch (e) {
            assertThat e, isA(InvalidArgumentsException)
            assertThat e, hasProperty('message',
                    containsString('parameter \'keyword_ids\' is an empty list'))
        }
    }
}
