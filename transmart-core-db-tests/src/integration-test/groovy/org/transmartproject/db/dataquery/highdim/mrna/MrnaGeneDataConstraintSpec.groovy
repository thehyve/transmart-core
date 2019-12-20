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
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.hamcrest.Matcher
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.i2b2data.PatientDimension
import spock.lang.Specification

import static org.hamcrest.Matchers.*
import static org.hibernate.sql.JoinType.INNER_JOIN
import static spock.util.matcher.HamcrestSupport.that

@Integration
@Rollback
class MrnaGeneDataConstraintSpec extends Specification {

    MrnaModule mrnaModule

    MrnaTestData testData = new MrnaTestData()

    void setupData() {
        testData.saveAll()
    }

    private HibernateCriteriaBuilder createCriteriaBuilder() {
        HibernateCriteriaBuilder builder = DeSubjectMicroarrayDataCoreDb.createCriteria()
        builder.buildCriteria {
            createAlias('probe', 'p', INNER_JOIN)

            eq 'trialName', MrnaTestData.TRIAL_NAME
        }
        builder
    }

    void basicTestGene() {
        setupData()
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.
                findAll({ it.keyword == 'BOGUSRQCD1' })*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        expect:
        that(res, allOf(
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
        ))
    }

    void testWithMultipleGenes() {
        setupData()
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* keywords ids for genes BOGUSCPO, BOGUSRQCD1 */
        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.findAll {
            ['BOGUSCPO', 'BOGUSRQCD1'].contains(it.keyword)
        }*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        expect:
        that(res, allOf(
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
        ))
    }

    private static Matcher matcherFor(PatientDimension patient, String gene) {
        allOf(
                hasProperty('probe',
                        hasProperty('geneSymbol', equalTo(gene))
                ),
                hasProperty('patient', equalTo(patient))
        )
    }

    void basicTestGeneSignature() {
        setupData()
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* should map to BOGUSVNN3 directly and to BOGUSRQCD1 via bio_assay_data_annotation
         * See comment before MrnaTestData.searchKeywords */
        def testee = mrnaModule.createDataConstraint([keyword_ids: testData.searchKeywords.
                findAll({ it.keyword == 'bogus_gene_sig_-602' })*.id],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        expect: '2 patients * 2 probes (one for each gene)'
        that(res,
                containsInAnyOrder(
                        matcherFor(testData.patients[0], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[1], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[0], 'BOGUSVNN3'),
                        matcherFor(testData.patients[1], 'BOGUSVNN3'),
                ))
    }

    void testMixedConstraint() {
        setupData()
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

        testee.doWithCriteriaBuilder(builder)
        List res = builder.instance.list()

        expect: '2 patients * 3 probes (one for each gene)'
        testee instanceof DisjunctionDataConstraint
        that(res,
                containsInAnyOrder(
                        matcherFor(testData.patients[0], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[1], 'BOGUSRQCD1'),
                        matcherFor(testData.patients[0], 'BOGUSVNN3'),
                        matcherFor(testData.patients[1], 'BOGUSVNN3'),
                        matcherFor(testData.patients[0], 'BOGUSCPO'),
                        matcherFor(testData.patients[1], 'BOGUSCPO'),
                ))
    }

    void testProteinConstraintByExternalId() {
        setupData()
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        /* there's a correlation between the
         * protein BOGUSCBPO_HUMAN and the gene BOGUSCPO */

        def testee = mrnaModule.createDataConstraint([ids: testData.bioMarkerTestData.proteinBioMarkers.
                find { it.name == 'BOGUSCBPO_HUMAN' }*.externalId],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        expect:
        that(res, everyItem(
                hasProperty('probe',
                        hasProperty('geneSymbol', equalTo('BOGUSCPO')))))
    }

    void testProteinConstraintByName() {
        setupData()
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = mrnaModule.createDataConstraint([names: ['BOGUSCBPO_HUMAN']],
                DataConstraint.PROTEINS_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        List res = builder.instance.list()

        expect:
        that(res, allOf(
                hasSize(2), /* 1 probe * 2 patients */
                everyItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', equalTo('BOGUSCPO'))))))
    }

    void testGeneSignatureConstraintByName() {
        setupData()
        HibernateCriteriaBuilder builder = createCriteriaBuilder()

        def testee = mrnaModule.createDataConstraint([names: ['bogus_gene_sig_-602']],
                DataConstraint.GENE_SIGNATURES_CONSTRAINT
        )

        testee.doWithCriteriaBuilder(builder)

        expect:
        that(builder.instance.list(), allOf(
                hasSize(4), /* 2 probes * 2 patients */
                everyItem(
                        hasProperty('probe',
                                hasProperty('geneSymbol', anyOf(
                                        equalTo('BOGUSVNN3'),
                                        equalTo('BOGUSRQCD1')))))))
    }

    void testEmptyConstraint() {
        setupData()

        when:
        mrnaModule.createDataConstraint([keyword_ids: []],
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT)
        then:
        def e = thrown(InvalidArgumentsException)
        e.message.contains('parameter \'keyword_ids\' is an empty list')
    }
}
