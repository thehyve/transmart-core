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

package org.transmartproject.db.dataquery.highdim.mirna

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
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationType
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.*

import javax.annotation.PostConstruct

import static org.hibernate.sql.JoinFragment.INNER_JOIN
import static org.transmartproject.db.util.GormWorkarounds.createCriteriaBuilder

/*
 * Mirna QPCR and Mirna SEQ are different data types (according to the user), but they have basically the same
 * implementation. We solve that by having a shared implementation in AbstractMirnaSharedModule.
 */

abstract class AbstractMirnaSharedModule extends AbstractHighDimensionDataTypeModule {

    final Map<String, Class> dataProperties = typesMap(DeSubjectMirnaData,
            ['rawIntensity', 'logIntensity', 'zscore'])

    final Map<String, Class> rowProperties = typesMap(MirnaProbeRow,
            ['probeId', 'mirnaId'])

    @Autowired
    StandardAssayConstraintFactory standardAssayConstraintFactory

    @Autowired
    StandardDataConstraintFactory standardDataConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @PostConstruct
    @Override
    void init() {
        super.init()
        correlationTypesRegistry.registerConstraint('MIRNA', 'mirnas')
        correlationTypesRegistry.registerCorrelation(
                new CorrelationType(name: 'MIRNA', sourceType: 'MIRNA', targetType: 'MIRNA'))
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Lazy private DataRetrievalParameterFactory searchKeywordDataConstraintFactory =
        new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                'MIRNA', 'p', 'mirnaId')

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ searchKeywordDataConstraintFactory,
                standardDataConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionsFactory(
                (Projection.LOG_INTENSITY_PROJECTION): 'logIntensity',
                (Projection.DEFAULT_REAL_PROJECTION): 'rawIntensity',
                (Projection.ZSCORE_PROJECTION):       'zscore'),
        new AllDataProjectionFactory(dataProperties, rowProperties)]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectMirnaData, 'm', session)

        criteriaBuilder.with {
            createAlias 'jProbe', 'p', INNER_JOIN

            projections {
                property 'assay.id',   'assayId'

                property 'p.id',       'probeId'
                property 'p.mirnaId',  'mirna'
                property 'p.detector', 'detector'
            }

            order 'p.id',     'asc'
            order 'assay.id', 'asc'

            // because we're using this transformer, every column has to have an alias
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
                rowsDimensionLabel:    'Probes',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                allowMissingAssays:    true,
                assayIdFromRow:        { it[0].assayId },
                inSameGroup:           { a, b -> a.probeId == b.probeId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    def firstNonNullCell = list.find()
                    new MirnaProbeRow(
                            probeId:       firstNonNullCell[0].probeId,
                            mirnaId:       firstNonNullCell[0].mirna,
                            assayIndexMap: assayIndexes,
                            data:          list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )
    }
}
