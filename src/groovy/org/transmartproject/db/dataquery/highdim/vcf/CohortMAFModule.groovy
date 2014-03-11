package org.transmartproject.db.dataquery.highdim.vcf

import org.hibernate.ScrollableResults
import grails.orm.HibernateCriteriaBuilder
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.vcf.*
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.hibernate.engine.SessionImplementor
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import static org.hibernate.sql.JoinFragment.INNER_JOIN
/**
 * Created by j.hudecek on 6-2-14.
 */
class CohortMAFModule extends AbstractHighDimensionDataTypeModule {

    static final String VALUES_PROJECTION = 'cohortMAF_values'

    final List<String> platformMarkerTypes = ['Chromosomal']

    final String name = 'cohortMAF'

    final String description = "cohortMAF_values data"

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    ChromosomeSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        chromosomeSegmentConstraintFactory.segmentPrefix = 'jDetail.'
        chromosomeSegmentConstraintFactory.segmentChromosomeColumn = 'chr'
        chromosomeSegmentConstraintFactory.segmentStartColumn      = 'pos'
        chromosomeSegmentConstraintFactory.segmentEndColumn        = 'pos'
        //customize the segment constraint factory to produce constraints targeting the right DeVariantSubjectSummaryCoreDb columns
        [
                standardDataConstraintFactory,
                chromosomeSegmentConstraintFactory
                //TODO: implement constraint on dataset
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [ //there needs to be a projection factory even though we're not using projections
                new MapBasedParameterFactory(
                        (VALUES_PROJECTION): { Map<String, Object> params ->
                            if (!params.isEmpty()) {
                                throw new InvalidArgumentsException('Expected no parameters here')
                            }
                            new VcfValuesProjection()
                        }
                )
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(DeVariantSubjectSummaryCoreDb, 'summary', session)

        criteriaBuilder.with {
            createAlias 'jDetail', 'p', INNER_JOIN
            projections {
                property 'summary.subjectId'       ,'subjectId'
                property 'summary.rsId'            ,'rsId'
                property 'summary.variant'         ,'variant'
                property 'summary.variantFormat'   ,'variantFormat'
                property 'summary.variantType'     ,'variantType'
                property 'summary.reference'       ,'reference'
                property 'summary.allele1'         ,'allele1'
                property 'summary.allele2'         ,'allele2'

                property 'p.chr'                   ,'chr'
                property 'p.pos'                   ,'pos'
                property 'p.rsId'                  ,'rsId'
                property 'p.ref'                   ,'ref'
                property 'p.alt'                   ,'alt'
                property 'p.quality'               ,'quality'
                property 'p.filter'                ,'filter'
                property 'p.info'                  ,'info'
                property 'p.format'                ,'format'
                property 'p.variant'               ,'variant'

                property 'assay.id'                ,'assayId'

            }

            order 'assay.id',  'asc' // important

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
                rowsDimensionLabel:    'Regions',
                columnsDimensionLabel: 'Sample codes',
                allowMissingAssays:    true,
                indicesList:           assays,
                results:               results,
                assayIdFromRow:        { it[0].assayId } ,
                inSameGroup:           { a, b -> a.chr == b.chr && a.pos == b.pos  },
                finalizeGroup:         { List collectedEntries -> /* list of all the results belonging to a group defined by inSameGroup */
                    /* list of arrays with one element: a map */
                    /* we may have nulls if allowMissingAssays is true,
                     *, but we're guaranteed to have at least one non-null */
                    return new VcfValuesImpl(collectedEntries)
                }
        )
    }
}
