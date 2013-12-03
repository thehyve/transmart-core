package org.transmartproject.db.dataquery.highdim.rbm

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
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionFactory

import static org.hibernate.sql.JoinFragment.INNER_JOIN

class RbmModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'rbm'

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [
                standardDataConstraintFactory,
                new SearchKeywordDataConstraintFactory(correlationTypesRegistry, 'PROTEIN', 'deRbmAnnotation', 'uniprotId'),
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionFactory('zscore') ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectRbmData, 'rbmdata', session)

        criteriaBuilder.with {
            createAlias 'deRbmAnnotation', 'p', INNER_JOIN

            projections {
                property 'assay', 'assay'

                property 'p.id', 'rbmAnnotationId'
                property 'p.uniprotId', 'uniprotId'
            }

            order 'p.id', 'asc'
            order 'assay.id', 'asc'
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }
        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {
        Map assayIndexes = createAssayIndexMap assays

        new DefaultHighDimensionTabularResult(
                rowsDimensionLabel: 'Antigenes',
                columnsDimensionLabel: 'Sample codes',
                indicesList: assays,
                results: results,
                inSameGroup: {a, b -> a.rbmAnnotationId == b.rbmAnnotationId},
                finalizeGroup: {List list ->
                    def firstNonNullCell = list.find()
                    new RbmRow(
                            rbmAnnotationId: firstNonNullCell[0].rbmAnnotationId,
                            uniprotId: firstNonNullCell[0].uniprotId,
                            assayIndexMap: assayIndexes,
                            data: list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )
    }
}
