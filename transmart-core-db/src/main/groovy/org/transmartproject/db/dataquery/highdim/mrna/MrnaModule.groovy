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
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.RepeatedEntriesCollectingTabularResult
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionsFactory

import static org.hibernate.sql.JoinType.INNER_JOIN

class MrnaModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'mrna'

    final String description = "Messenger RNA data (Microarray)"

    final List<String> platformMarkerTypes = ['Gene Expression']

    final Map<String, Class> dataProperties = typesMap(DeSubjectMicroarrayDataCoreDb,
            ['trialName', 'rawIntensity', 'logIntensity', 'zscore'])

    final Map<String, Class> rowProperties = typesMap(ProbeRow,
            ['probe', 'geneId', 'geneSymbol'])

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection,
                                              SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectMicroarrayDataCoreDb, 'mrnadata', session)

        criteriaBuilder.with {
            createAlias 'jProbe', 'p', INNER_JOIN

            projections {
                property 'assay.id',     'assayId'

                property 'p.id',         'probeId'
                property 'p.probeId',    'probeName'
                property 'p.geneSymbol', 'geneSymbol'
                property 'p.geneId',     'geneId'
                property 'p.organism',   'organism'
            }

            order 'p.id',         'asc'
            order 'p.geneSymbol', 'asc' // see below
            order 'assay.id',     'asc' // important! See assumption below

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    @CompileStatic
    TabularResult transformResults(ScrollableResults results,
                                     List<AssayColumn> assays,
                                     Projection projection) {
        /* assumption here is the assays in the passed in list are in the same
         * order as the assays in the result set */
        Map assayIndexMap = createAssayIndexMap assays

        def preliminaryResult = new DefaultHighDimensionTabularResult<ProbeRow>(
                rowsDimensionLabel:    'Probes',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                allowMissingAssays:    true
            ) {
                @Override @CompileStatic
                def assayIdFromRow(Map row) { row.assayId }

                @Override @CompileStatic
                boolean inSameGroup(Map a, Map b) {a.probeId == b.probeId && a.geneSymbol == b.geneSymbol }

                @Override @CompileStatic
                ProbeRow finalizeRow(List<Map> list /* list of arrays with one element: a map */) {
                    /* we may have nulls if allowMissingAssays is true,
                     * but we're guaranteed to have at least one non-null */
                    Map firstNonNullCell = findFirst list
                    new ProbeRow(
                            probe:         (String) firstNonNullCell.probeName,
                            geneSymbol:    (String) firstNonNullCell.geneSymbol,
                            geneId:        (String) firstNonNullCell.geneId,
                            assayIndexMap: assayIndexMap,
                            data:          doWithProjection(projection, list),
                    )
                }
        }

        /* In some implementations, probeset_id is actually not a primary key on
         * the annotations table and several rows will be returned for the same
         * probeset_id, just with different genes.
         * Hence the order by clause and the definition of inSameGroup above */
        new RepeatedEntriesCollectingTabularResult<ProbeRow>(preliminaryResult) {
            @Override @CompileStatic def collectBy(ProbeRow it) { it.probe }
            @Override @CompileStatic ProbeRow resultItem(List<ProbeRow> collectedList) {
                if (collectedList) {
                    new ProbeRow(
                            probe: collectedList[0].probe,
                            geneSymbol: safeJoin(collectedList*.geneSymbol, '/'),
                            geneId: safeJoin(collectedList*.geneId, '/'),
                            assayIndexMap: collectedList[0].assayIndexMap,
                            data: collectedList[0].data
                    )
                } else {null}
            }
        }
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ standardDataConstraintFactory,
                new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                        'GENE', 'p', 'geneId') ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionsFactory(
                (Projection.LOG_INTENSITY_PROJECTION): 'logIntensity',
                (Projection.DEFAULT_REAL_PROJECTION): 'rawIntensity',
                (Projection.ZSCORE_PROJECTION):       'zscore'),
        new AllDataProjectionFactory(dataProperties, rowProperties)]
    }
}
