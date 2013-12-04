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
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardAssayConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardDataConstraintFactory

import javax.annotation.PostConstruct

import static org.hibernate.sql.JoinFragment.INNER_JOIN

class MirnaModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'mirna'

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
                'MIRNA', 'jProbe', 'mirnaId')

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ searchKeywordDataConstraintFactory,
                standardDataConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionFactory('zscore') ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectMirnaData, 'm', session)

        criteriaBuilder.with {
            createAlias 'jProbe', 'p', INNER_JOIN

            projections {
                property 'assay',      'assay'

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
                assayIdFromRow:        { it[0].assay.id },
                inSameGroup:           { a, b -> a.probeId == b.probeId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    def firstNonNullCell = list.find()
                    new MirnaProbeRow(
                            label:         firstNonNullCell[0].probeId ?: firstNonNullCell[0].detector,
                            assayIndexMap: assayIndexes,
                            data:          list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )
    }
}
