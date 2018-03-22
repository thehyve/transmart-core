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

package org.transmartproject.db.dataquery.highdim.rnaseqcog

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
import org.transmartproject.db.dataquery.highdim.parameterproducers.*

import static org.hibernate.sql.JoinFragment.INNER_JOIN

/**
 * Module for RNA-seq, as implemented for Oracle by Cognizant.
 * This name is to distinguish it from the TraIT implementation.
 */
class RnaSeqCogModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'rnaseq_cog'

    final String description = "Messenger RNA data (Sequencing)"

    final List<String> platformMarkerTypes = ['RNASEQ']

    final Map<String, Class> dataProperties = typesMap(DeSubjectRnaData,
            ['rawIntensity', 'logIntensity', 'zscore'])

    final Map<String, Class> rowProperties = typesMap(RnaSeqCogDataRow,
            ['annotationId', 'geneSymbol', 'geneId'])

    @Autowired
    StandardAssayConstraintFactory standardAssayConstraintFactory

    @Autowired
    StandardDataConstraintFactory standardDataConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectRnaData, 'rnadata', session)

        criteriaBuilder.with {
            createAlias 'jAnnotation', 'ann', INNER_JOIN

            projections {
                property 'assay.id',         'assayId'

                property 'ann.id',           'annotationId'
                property 'ann.geneSymbol',   'geneSymbol'
                property 'ann.geneId',       'geneId'
            }

            order 'ann.id',         'asc'
            order 'ann.geneSymbol', 'asc'
            order 'assay.id',       'asc' // important! See assumption below

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @CompileStatic
    @Override
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {
        Map assayIndexMap = createAssayIndexMap assays

        def preliminaryResult = new DefaultHighDimensionTabularResult<RnaSeqCogDataRow>(
                rowsDimensionLabel: 'Transcripts',
                columnsDimensionLabel: 'Sample codes',
                indicesList: assays,
                results: results,
                allowMissingAssays: true,
            ) {
                @Override
                def assayIdFromRow(Object[] row) { row[0].assayId }

                @Override
                boolean inSameGroup(a, b) { a.annotationId == b.annotationId && a.geneSymbol == b.geneSymbol }

                @Override
                RnaSeqCogDataRow finalizeGroup(List<Object[]> list /* list of arrays with one element: a map */) {
                    Map firstNonNullCell = (Map) list.find()[0]
                    new RnaSeqCogDataRow(
                            annotationId: firstNonNullCell.annotationId,
                            geneSymbol: firstNonNullCell.geneSymbol,
                            geneId: firstNonNullCell.geneId,
                            assayIndexMap: assayIndexMap,
                            data: doWithProjection(projection, list)
                    )
                }
        }

        new RepeatedEntriesCollectingTabularResult<RnaSeqCogDataRow>(preliminaryResult) {
            @Override
            def collectBy(RnaSeqCogDataRow it) { it.annotationId }

            @Override
            RnaSeqCogDataRow resultItem(List<RnaSeqCogDataRow> collectedList) {
                if (collectedList) {
                    new RnaSeqCogDataRow(
                            annotationId: collectedList[0].annotationId,
                            geneSymbol: safeJoin(collectedList*.geneSymbol, '/'),
                            geneId: safeJoin(collectedList*.geneId, '/'),
                            assayIndexMap: collectedList[0].assayIndexMap,
                            data: collectedList[0].data
                    )
                }
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
                        'GENE', 'ann', 'geneId') ]
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
