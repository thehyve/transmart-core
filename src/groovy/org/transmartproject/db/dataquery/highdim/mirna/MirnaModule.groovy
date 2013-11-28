package org.transmartproject.db.dataquery.highdim.mirna

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.dataconstraints.PropertyDataConstraint
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardAssayConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection

import static org.hibernate.sql.JoinFragment.INNER_JOIN

class MirnaModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'mirna'

    @Autowired
    StandardAssayConstraintFactory standardAssayConstraintFactory

    @Autowired
    StandardDataConstraintFactory standardDataConstraintFactory

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    private DataRetrievalParameterFactory dataConstraintFactory = new MapBasedParameterFactory(
            mirnas: { Map params ->
                if (params.isEmpty() || params.names == null) {
                    throw new InvalidArgumentsException(
                            "Expected a single param, 'names', got ${params.keySet()}")
                }
                boolean paramsOk = params.names instanceof List &&
                        params.names.every { it instanceof String }
                if (!paramsOk) {
                    throw new InvalidArgumentsException("Expected the parameter " +
                            "'names' to be a list of strings, but got ${params.names}")
                }

                new PropertyDataConstraint(
                        property: 'p.mirnaId',
                        values:   params.names)
            }
    )

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ dataConstraintFactory,
                standardDataConstraintFactory ]
    }

    private DataRetrievalParameterFactory projectionsFactory = new MapBasedParameterFactory(
            (CriteriaProjection.DEFAULT_REAL_PROJECTION): { Map params ->
                if (!params.isEmpty()) {
                    throw new InvalidArgumentsException(
                            'This projection takes no parameters')
                }

                new SimpleRealProjection(property: 'zscore')
            }
    )

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ projectionsFactory ]
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
                inSameGroup:           { a, b -> a.probeId == b.probeId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    def firstNonNullCell = list.find()
                    new MirnaProbeRow(
                            label:         firstNonNullCell[0].mirna ?: firstNonNullCell[0].detector,
                            assayIndexMap: assayIndexes,
                            data:          list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )
    }
}
