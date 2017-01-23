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

package org.transmartproject.db.dataquery.highdim.protein

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.PlatformImpl
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionsFactory

import static org.hibernate.sql.JoinFragment.INNER_JOIN
import static org.transmartproject.db.util.GormWorkarounds.createCriteriaBuilder

class ProteinModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'protein'

    final String description = "Proteomics data (Mass Spec)"

    final List<String> platformMarkerTypes = ['PROTEOMICS']

    final Map<String, Class> dataProperties = typesMap(DeSubjectProteinData,
            ['intensity', 'logIntensity', 'zscore'])

    final Map<String, Class> rowProperties = typesMap(ProteinDataRow,
            ['uniprotName', 'peptide'])

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Lazy private DataRetrievalParameterFactory searchKeywordDataConstraintFactory =
        new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                'PROTEIN', 'a', 'uniprotId')

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ searchKeywordDataConstraintFactory,
                standardDataConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionsFactory(
                (Projection.LOG_INTENSITY_PROJECTION): 'logIntensity',
                (Projection.DEFAULT_REAL_PROJECTION):  'intensity',
                (Projection.ZSCORE_PROJECTION):        'zscore'),
        new AllDataProjectionFactory(dataProperties, rowProperties)]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection,
                                              SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectProteinData, 'proteindata', session)

        criteriaBuilder.with {
            createAlias 'jAnnotation', 'a', INNER_JOIN
            createAlias 'jAnnotation.platform', 'platform', INNER_JOIN

            projections {
                property 'assay.id',      'assayId'

                property 'a.id',          'annotationId'
                property 'a.uniprotName', 'uniprotName'
                property 'a.peptide',     'peptide'
                property 'a.chromosome',  'chromosome'
                property 'a.startBp',     'startBp'
                property 'a.endBp',       'endBp'

                property 'platform.id',              'platformId'
                property 'platform.title',           'platformTitle'
                property 'platform.organism',        'platformOrganism'
                property 'platform.annotationDate',  'platformAnnotationDate'
                property 'platform.markerType',      'platformMarkerType'
                property 'platform.genomeReleaseId', 'platformGenomeReleaseId'
            }

            order 'a.id',         'asc'
            order 'assay.id',     'asc'
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }
        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results,
                                   List<AssayColumn> assays,
                                   Projection projection) {
        Map assayIndexes = createAssayIndexMap assays

        new DefaultHighDimensionTabularResult(
                rowsDimensionLabel:    'Proteins',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                allowMissingAssays:    true,
                assayIdFromRow:        { it[0].assayId },
                inSameGroup:           { a, b -> a.annotationId == b.annotationId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    def cell = list.find()[0]
                    new ProteinDataRow(
                            id:            cell.annotationId,
                            peptide:       cell.peptide,
                            uniprotName:   cell.uniprotName,
                            platform:      new PlatformImpl(
                                                id:              cell.platformId,
                                                title:           cell.platformTitle,
                                                organism:        cell.platformOrganism,
                                                //It converts timestamp to date
                                                annotationDate:  cell.platformAnnotationDate ?
                                                        new Date(cell.platformAnnotationDate.getTime())
                                                        : null,
                                                markerType:      cell.platformMarkerType,
                                                genomeReleaseId: cell.platformGenomeReleaseId
                            ),
                            chromosome:     cell.chromosome,
                            start:          cell.startBp,
                            end:            cell.endBp,
                            assayIndexMap: assayIndexes,
                            data:          list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )
    }
}
