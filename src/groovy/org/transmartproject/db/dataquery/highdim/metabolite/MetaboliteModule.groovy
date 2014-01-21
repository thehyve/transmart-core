package org.transmartproject.db.dataquery.highdim.metabolite

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
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionsFactory

class MetaboliteModule extends AbstractHighDimensionDataTypeModule {
    final String name = 'metabolite'

    final List<String> platformMarkerTypes = ['METABOLOMICS']

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [ standardDataConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ new SimpleRealProjectionsFactory(
                (Projection.DEFAULT_REAL_PROJECTION): 'raw_intensity',
                (Projection.ZSCORE_PROJECTION):       'zscore') ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection,
                                              SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectMetabolomicsData, 'metabolitedata', session)

        criteriaBuilder.with {
            createAlias 'jAnnotation', 'a', INNER_JOIN

            projections {
                property 'assay',             'assay'
                property 'a.id',              'annotationId'
                property 'a.hmdbId',          'hmdbId'
                property 'a.biochemicalName', 'biochemicalName'
            }

            order 'a.id',     'asc'
            order 'assay.id', 'asc'
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
                rowsDimensionLabel:    'Metabolites',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                allowMissingAssays:    true,
                assayIdFromRow:        { it[0].assay.id },
                inSameGroup:           { a, b -> a.annotationId == b.annotationId },
                finalizeGroup:         { List list -> /* list of arrays with one element: a map */
                    def firstNonNullCell = list.find()
                    new MetaboliteDataRow(
                            biochemicalName: firstNonNullCell[0].biochemicalName,
                            hmdbId:          firstNonNullCell[0].hmdbId,
                            assayIndexMap:   assayIndexes,
                            data:            list.collect { projection.doWithResult it?.getAt(0) }
                    )
                }
        )
    }
}