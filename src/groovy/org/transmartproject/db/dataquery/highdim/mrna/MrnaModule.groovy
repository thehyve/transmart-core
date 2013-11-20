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
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
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
        int i = 0
        Map assayIndexMap = assays.collectEntries { [ it, i++ ] }

        new DefaultHighDimensionTabularResult(
                rowsDimensionLabel:    'Probes',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                inSameGroup:           { a, b -> a.probeId == b.probeId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    if (list.size() != assays.size()) {
                        throw new UnexpectedResultException(
                                "Expected group to be of size ${assays.size()}; got ${list.size()} objects")
                    }
                    new ProbeRow(
                            probe:         list[0][0].probeName,
                            geneSymbol:    list[0][0].geneSymbol,
                            organism:      list[0][0].organism,
                            assayIndexMap: assayIndexMap,
                            data:          list.collect { projection.doWithResult it[0] }
                    )
                }
        )

    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    static class MrnaDataConstraintsProducers extends AbstractMethodBasedParameterFactory {
        @ProducerFor(DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT)
        DataConstraint createSearchKeywordIdsConstraint(Map<String, Object> params) {
            if (params.size() != 1) {
                throw new InvalidArgumentsException("Expected exactly one parameter (keyword_ids), got $params")
            }

            def keywords = getParam params, 'keyword_ids', List
            keywords = keywords.collect { convertToLong 'element of keyword_ids', it }

            MrnaGeneDataConstraint.createForSearchKeywordIds(keywords)
        }
    }


    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ standardDataConstraintFactory,
                new MrnaDataConstraintsProducers() ]
    }

    private final DataRetrievalParameterFactory defaultRealProjectionFactory =
            new MapBasedParameterFactory(
                (CriteriaProjection.DEFAULT_REAL_PROJECTION): { Map params ->
                    if (!params.isEmpty())
                        throw new InvalidArgumentsException("No params expected here, got $params")

                    new SimpleRealProjection('logIntensity')
                }
            )

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ defaultRealProjectionFactory ]
    }
}
