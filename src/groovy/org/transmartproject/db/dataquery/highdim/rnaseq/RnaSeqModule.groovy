package org.transmartproject.db.dataquery.highdim.rnaseq

import com.google.common.collect.ImmutableSet
import grails.orm.HibernateCriteriaBuilder

import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.chromoregion.RegionRowImpl
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.acgh.AcghDataTypeResource

import static org.hibernate.sql.JoinFragment.INNER_JOIN

/**
 * Module for RNA-seq, as implemented for Postgres by TraIT.
 */
class RnaSeqModule extends AbstractHighDimensionDataTypeModule {

    static final String RNASEQ_VALUES_PROJECTION = 'rnaseq_values'

    final List<String> platformMarkerTypes = ['Chromosomal']

    final String name = 'rnaseq'

    final String description = "RNA Sequence data"

    private final Set<String> dataProperties = ImmutableSet.of('trialName', 'readCount')

    private final Set<String> rowProperties = ImmutableSet.of()

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    ChromosomeSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Override
    HighDimensionDataTypeResource createHighDimensionResource(Map params) {
        /* return instead subclass of HighDimensionDataTypeResourceImpl,
         * because we add a method, retrieveChromosomalSegments() */
        new AcghDataTypeResource(this)
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [
                standardDataConstraintFactory,
                chromosomeSegmentConstraintFactory
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [
                new MapBasedParameterFactory(
                        (RNASEQ_VALUES_PROJECTION): { Map<String, Object> params ->
                            if (!params.isEmpty()) {
                                throw new InvalidArgumentsException('Expected no parameters here')
                            }
                            new RnaSeqValuesProjection()
                        }
                ),
                new AllDataProjectionFactory(dataProperties, rowProperties)
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
            createCriteriaBuilder(DeSubjectRnaseqData, 'rnaseq', session)

        criteriaBuilder.with {
            createAlias 'jRegion', 'region', INNER_JOIN

            projections {
                property 'rnaseq.assay.id'
                property 'rnaseq.readCount'

                property 'region.id'
                property 'region.name'
                property 'region.cytoband'
                property 'region.chromosome'
                property 'region.start'
                property 'region.end'
                property 'region.numberOfProbes'
            }

            order 'region.id', 'asc'
            order 'assay.id',  'asc' // important
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
                rowsDimensionLabel:    'Regions',
                columnsDimensionLabel: 'Sample codes',
                indicesList:           assays,
                results:               results,
                inSameGroup:           { a, b -> a[2] == b[2] /* region.id */ },
                finalizeGroup:         { List list -> /* list of arrays with 9 elements (1/projection) */
                    if (list.size() != assays.size()) {
                        throw new UnexpectedResultException(
                                "Expected group to be of size ${assays.size()}; got ${list.size()} objects")
                    }
                    def regionRow = new RegionRowImpl(Arrays.asList(list[0])[2..8])
                    regionRow.assayIndexMap = assayIndexMap
                    regionRow.data = list.collect {
                        projection.doWithResult(Arrays.asList(it)[0..1])
                    }
                    regionRow
                }
        )
    }
}
