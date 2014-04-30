package org.transmartproject.db.dataquery.highdim.vcf

import static org.hibernate.sql.JoinFragment.INNER_JOIN
import grails.orm.HibernateCriteriaBuilder

import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory

class VCFModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'vcf'

    final String description = "Genomic Variant data"

    final List<String> platformMarkerTypes = ['VCF']

    // VCF specific projection names
    static final String VCF_PROJECTION       = 'vcf'
    static final String COHORT_PROJECTION    = 'cohort'
    
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
        [ 
            new MapBasedParameterFactory(
                (COHORT_PROJECTION): { Map<String, Object> params ->
                    if (!params.isEmpty()) {
                        throw new InvalidArgumentsException('Expected no parameters here')
                    }
                    new CohortProjection()
                },
                (VCF_PROJECTION): { Map<String, Object> params ->
                    if (!params.isEmpty()) {
                        throw new InvalidArgumentsException('Expected no parameters here')
                    }
                    new VCFProjection()
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
            order 'chr',  'asc'
            order 'pos',  'asc'
            order 'assayId',  'asc' // important

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {
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
                inSameGroup:           { a, b -> a[0].chr == b[0].chr && a[0].pos == b[0].pos  },
                finalizeGroup:         { List collectedEntries -> /* list of all the results belonging to a group defined by inSameGroup */
                    /* list of arrays with one element: a map */
                    /* we may have nulls if allowMissingAssays is true,
                    *, but we're guaranteed to have at least one non-null */
                    projection.doWithResult(collectedEntries)
                }
        )
    }
}
