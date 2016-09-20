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

import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.sql.Sql
import org.hibernate.Session
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.HighDimTestData
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.assayconstraints.AssayIdListCriteriaConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultTrialNameCriteriaConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.DisjunctionAssayCriteriaConstraint
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.CriteriaDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection
import spock.lang.Ignore
import spock.lang.Specification

import javax.sql.DataSource

import static org.hamcrest.Matchers.*
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createTestAssays
import static org.transmartproject.db.test.Matchers.hasSameInterfaceProperties

@Integration
@Rollback

class MrnaDataRetrievalSpec extends Specification {

    private static final double DELTA = 0.0001

    @Autowired
    @Qualifier('mrnaModule')
    HighDimensionDataTypeModule mrnaModule

    def sessionFactory

    DataSource dataSource

    HighDimensionDataTypeResource resource

    TabularResult dataQueryResult

    CorrelationTypesRegistry correlationTypesRegistry

    MrnaTestData testData

    DefaultTrialNameCriteriaConstraint trialNameConstraint =
            new DefaultTrialNameCriteriaConstraint(trialName: MrnaTestData.TRIAL_NAME)

    SimpleRealProjection rawIntensityProjection =
            new SimpleRealProjection(property: 'rawIntensity')

    void setupData() {
        testData = new MrnaTestData()
        testData.saveAll()

        assert mrnaModule != null

        resource = new HighDimensionDataTypeResourceImpl(mrnaModule)
    }

    void cleanup() {
        if (dataQueryResult) {
            dataQueryResult.close()
        }
    }

    void basicTest() {
        setupData()
        trialNameConstraint = new DefaultTrialNameCriteriaConstraint(trialName: MrnaTestData.TRIAL_NAME)
        List assayConstraints = [trialNameConstraint]
        List dataConstraints = []

        when:
        dataQueryResult =
                resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection

        then:
        dataQueryResult allOf(
                hasProperty('columnsDimensionLabel', equalTo('Sample codes')),
                hasProperty('rowsDimensionLabel', equalTo('Probes')),
        )

        when:
        def resultList = Lists.newArrayList dataQueryResult

        then:
        resultList allOf(
                hasSize(3),
                everyItem(isA(ProbeRow)),
                everyItem(
                        hasProperty('data',
                                allOf(
                                        hasSize(2),
                                        everyItem(isA(Double))
                                )
                        )
                ),
                everyItem(
                        hasProperty('assayIndexMap', allOf(
                                isA(Map),
                                hasEntry(
                                        /* assays are sorted in ascending order,
                                         * so -402 comes before -401 */
                                        hasProperty('id', equalTo(-402L)), /* key */
                                        equalTo(0),                        /* value */
                                ),
                                hasEntry(
                                        hasProperty('id', equalTo(-401L)),
                                        equalTo(1),
                                ),
                        ))
                ),
                contains(
                        allOf(
                                hasProperty('bioMarker', equalTo('BOGUSVNN3')),
                                hasProperty('label', equalTo('1553513_at')),
                        ),
                        hasProperty('bioMarker', equalTo('BOGUSRQCD1')),
                        hasProperty('bioMarker', equalTo('BOGUSCPO')),
                )
        )

        when:
        ProbeRow firstProbe = resultList.first()
        List<AssayColumn> lac = dataQueryResult.indicesList

        then:
        firstProbe.assayIndexMap.entrySet() hasSize(2)

        // first probe is 1553510_s_at (gene BOGUSVNN3), as asserted before
        // intensities are 0.5 and 0.6
        firstProbe[0] allOf(
                closeTo(firstProbe[lac.find { it.id == -402L /*ascending order*/ }], DELTA),
                closeTo(0.6f, DELTA)
        )
        firstProbe[1] allOf(
                closeTo(firstProbe[lac.find { it.id == -401L }], DELTA),
                closeTo(0.5f, DELTA)
        )
    }

    private CriteriaDataConstraint createGenesDataConstraint(List skIds) {
        SearchKeywordDataConstraint.createForSearchKeywordIds(
                entityAlias: 'p',
                propertyToRestrict: 'geneId',
                correlationTypes:
                        correlationTypesRegistry.getCorrelationTypesForTargetType('GENE'),
                skIds)
    }

    void testWithGeneConstraint() {
        setupData()
        List assayConstraints = [trialNameConstraint]
        List dataConstraints = [
                createGenesDataConstraint([
                        testData.searchKeywords.
                                find({ it.keyword == 'BOGUSRQCD1' }).id
                ])
        ]
        dataQueryResult =
                resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection

        def resultList = Lists.newArrayList dataQueryResult

        expect:
        resultList allOf(
                hasSize(1),
                everyItem(hasProperty('data', hasSize(2))),
                contains(hasProperty('bioMarker', equalTo('BOGUSRQCD1')))
        )
    }

    void testWithDisjunctionAssayConstraint() {
        setupData()
        List assayConstraints = [
                new DisjunctionAssayCriteriaConstraint(constraints: [
                        new AssayIdListCriteriaConstraint(ids: [testData.assays[0].id]),
                        new AssayIdListCriteriaConstraint(ids: [testData.assays[1].id])])]

        dataQueryResult =
                resource.retrieveData assayConstraints, [], rawIntensityProjection

        expect:
        dataQueryResult.indicesList contains(
                hasProperty('id', is(testData.assays[1].id)),
                hasProperty('id', is(testData.assays[0].id)))
    }

    void testWithDisjunctionDataConstraint() {
        setupData()
        List assayConstraints = [trialNameConstraint]
        /* in this particular case, you could just use one constraint
         * and include two ids in the list */
        List dataConstraints = [
                new DisjunctionDataConstraint(constraints: [
                        createGenesDataConstraint([
                                testData.searchKeywords.find({ it.keyword == 'BOGUSRQCD1' }).id
                        ]),
                        createGenesDataConstraint([
                                testData.searchKeywords.find({ it.keyword == 'BOGUSVNN3' }).id
                        ])
                ])
        ]

        dataQueryResult =
                resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection

        def resultList = Lists.newArrayList dataQueryResult

        expect:
        resultList containsInAnyOrder(
                hasProperty('bioMarker', equalTo('BOGUSRQCD1')),
                hasProperty('bioMarker', equalTo('BOGUSVNN3')),
        )
    }

    private TabularResult testWithMissingDataAssay(Long baseAssayId) {
        setupData()
        def extraAssays = createTestAssays([testData.patients[0]], baseAssayId,
                testData.platform, MrnaTestData.TRIAL_NAME)
        HighDimTestData.save extraAssays

        List assayConstraints = [trialNameConstraint]

        dataQueryResult =
                resource.retrieveData assayConstraints, [], rawIntensityProjection
    }

    void testWithMissingAssayLowestIdNumber() {
        setupData()
        testWithMissingDataAssay(-50000L)

        expect:
        dataQueryResult.indicesList[0]
        hasSameInterfaceProperties(Assay, DeSubjectSampleMapping.get(-50001L))

        Lists.newArrayList(dataQueryResult.rows) everyItem(
                hasProperty('data', allOf(
                        hasSize(3), // for the three assays
                        contains(
                                is(nullValue()),
                                is(notNullValue()),
                                is(notNullValue()),
                        )
                ))
        )
    }

    void testWithMissingAssayHighestIdNumber() {
        setupData()
        testWithMissingDataAssay(5000000L)

        expect:
        dataQueryResult.indicesList[2]
        hasSameInterfaceProperties(Assay, DeSubjectSampleMapping.get(4999999L))

        Lists.newArrayList(dataQueryResult.rows) everyItem(
                hasProperty('data', allOf(
                        hasSize(3), // for the three assays
                        contains(
                                is(notNullValue()),
                                is(notNullValue()),
                                is(nullValue()),
                        )
                ))
        )
    }

    void testWithMissingAssayAllowMissingAssays() {
        setupData()
        testWithMissingDataAssay(-50000L)
        expect:
        Lists.newArrayList(dataQueryResult.rows) everyItem(
                hasProperty('data', allOf(
                        hasSize(3), // for the three assays
                        contains(
                                is(nullValue()),
                                is(notNullValue()),
                                is(notNullValue()),
                        )
                ))
        )
    }

    @Ignore
    // this somehow breaks 3 tests in MrnaGeneDataConstraintTests
    // saying the column correl.correl_type does not exist (!)
    void testWithDuplicateProbes() {
        setupData()
        /* this tests support for a schema variation where probeset_id is not
         * a primary key for the annotation table and actually the same probe
         * can be repeated but with different genes associated */

        /* The ALTER TABLE statements commit the transaction and therefore the
         * data won't be cleaned up afterwards.
         * Try to save the database state and restore it later */
        Session session = sessionFactory.currentSession
        File schemaDump = File.createTempFile("coredb-database-dump", ".sql")
        Sql sql = new Sql(dataSource)
        sql.execute "SCRIPT DROP TO '${schemaDump.absolutePath}'"

        String constraint = session.createSQLQuery('''
                SELECT constraint_name FROM information_schema.constraints
                WHERE table_name = 'DE_SUBJECT_MICROARRAY_DATA' AND column_list = 'PROBESET_ID'
        ''').list()[0]
        runStatement "ALTER TABLE deapp.de_subject_microarray_data DROP CONSTRAINT $constraint"
        runStatement 'ALTER TABLE deapp.de_mrna_annotation DROP PRIMARY KEY'
        try {
            runStatement """
                    insert into deapp.de_mrna_annotation(probeset_id, probe_id, gene_symbol, gene_id)
                    values('${testData.annotations[0].id}',
                            '${testData.annotations[0].probeId}',
                            'Z_EXTRA_GENE_SYMB',
                            '0')
                    """

            trialNameConstraint = new DefaultTrialNameCriteriaConstraint(trialName: MrnaTestData.TRIAL_NAME)
            List assayConstraints = [trialNameConstraint]
            List dataConstraints = []

            dataQueryResult =
                    resource.retrieveData assayConstraints, dataConstraints, rawIntensityProjection


            ArrayList results = Lists.newArrayList(dataQueryResult)
            expect:
            results allOf(
                    hasSize(3),
                    hasItem(allOf(
                            hasProperty('geneSymbol',
                                    anyOf(
                                            equalTo('BOGUSCPO/Z_EXTRA_GENE_SYMB'),
                                            equalTo('Z_EXTRA_GENE_SYMB/BOGUSCPO'))),
                            contains(
                                    testData.microarrayData.
                                            findAll { it.probe.geneSymbol == 'BOGUSCPO' }.
                                            sort { it.assay.id }.
                                            collect {
                                                closeTo(it.rawIntensity as Double, DELTA)
                                            }))))
        } finally {
            sql.execute "RUNSCRIPT FROM ${schemaDump.absolutePath}"
            session.clear()
            schemaDump.delete()
        }
    }

    private runStatement(String statement) {
        sessionFactory.currentSession.createSQLQuery(statement).executeUpdate()
    }
}
