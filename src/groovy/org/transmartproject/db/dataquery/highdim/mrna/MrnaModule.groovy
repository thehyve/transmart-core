package org.transmartproject.db.dataquery.highdim.mrna

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraint
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AbstractMethodBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.ProducerFor
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import org.transmartproject.db.dataquery.highdim.projections.SimpleRealProjection

import static org.hibernate.sql.JoinFragment.INNER_JOIN

class MrnaModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'mrna'

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
                property 'assay',        'assay'

                property 'p.id',         'probeId'
                property 'p.probeId',    'probeName'
                property 'p.geneSymbol', 'geneSymbol'
                property 'p.organism',   'organism'
            }

            order 'p.id',     'asc'
            order 'assay.id', 'asc' // important! See assumption below

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results,
                                     List<AssayColumn> assays,
                                     Projection projection) {
        /* assumption here is the assays in the passed in list are in the same
         * order as the assays in the result set */
        Map assayIndexMap = createAssayIndexMap assays

        new DefaultHighDimensionTabularResult(
                rowsDimensionLabel:    'Probes',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                allowMissingAssays:    false,
                assayIdFromRow:        { it[0].assay.id },
                inSameGroup:           { a, b -> a.probeId == b.probeId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    /* we may have nulls if allowMissingAssays is true,
                     * but we're guaranteed to have at least one non-null */
                    def firstNonNullCell = list.find()
                    new ProbeRow(
                            probe:         firstNonNullCell[0].probeName,
                            geneSymbol:    firstNonNullCell[0].geneSymbol,
                            organism:      firstNonNullCell[0].organism,
                            assayIndexMap: assayIndexMap,
                            data:          list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )

    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ standardDataConstraintFactory,
                new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                        'GENE', 'jProbe', 'geneId') ]
    }

    private final DataRetrievalParameterFactory defaultRealProjectionFactory =
            new MapBasedParameterFactory(
                (CriteriaProjection.DEFAULT_REAL_PROJECTION): { Map params ->
                    if (!params.isEmpty())
                        throw new InvalidArgumentsException("No params expected here, got $params")

                    new SimpleRealProjection('zscore')
                }
            )

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ defaultRealProjectionFactory ]
    }
}
