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

package org.transmartproject.db.dataquery.highdim.vcf

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
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory

import static org.transmartproject.db.util.GormWorkarounds.createCriteriaBuilder

class VcfModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'vcf'

    final String description = "Genomic Variant data"

    final List<String> platformMarkerTypes = ['VCF']

    // VCF specific projection names

    /**
     * Projection to use if you want to compute cohort level properties (such as MAF)
     */
    static final String COHORT_PROJECTION    = 'cohort'

    /**
     * Projection that simply returns the variant (e.g. CTC) for each subject
     */
    static final String VARIANT_PROJECTION    = 'variant'

    final Map<String, Class> dataProperties = typesMap(DeVariantSubjectSummaryCoreDb,
    ['reference', 'variant', 'variantType'])

    final Map<String, Class> rowProperties = typesMap(VcfDataRow,
    ['chromosome', 'position', 'rsId', 'referenceAllele'])

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    ChromosomeSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [ standardAssayConstraintFactory ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        chromosomeSegmentConstraintFactory.segmentPrefix           = 'summary.'
        chromosomeSegmentConstraintFactory.segmentChromosomeColumn = 'chr'
        chromosomeSegmentConstraintFactory.segmentStartColumn      = 'pos'
        chromosomeSegmentConstraintFactory.segmentEndColumn        = 'pos'
        //customize the segment constraint factory to produce constraints targeting the right DeVariantSubjectSummaryCoreDb columns
        [
            standardDataConstraintFactory,
            chromosomeSegmentConstraintFactory,
            new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                    'GENE', 'summary', 'geneId')
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
                        (VARIANT_PROJECTION): { Map<String, Object> params ->
                            if (!params.isEmpty()) {
                                throw new InvalidArgumentsException('Expected no parameters here')
                            }
                            new VariantProjection()
                        }
                ),
                new AllDataProjectionFactory(dataProperties, rowProperties)
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(DeVariantSummaryDetailGene, 'summary', session)

        criteriaBuilder.with {
            projections {
                // These fields are needed to fill the VcfDataRow
                // Fields describing the actual data are added by
                // the projections
                property 'dataset.id'       ,'dataset_id'
                property 'chr'              ,'chr'
                property 'pos'              ,'pos'
                property 'rsId'             ,'rsId'
                property 'reference'        ,'reference'

                property 'ref'              ,'ref'
                property 'alt'              ,'alt'
                property 'quality'          ,'quality'
                property 'filter'           ,'filter'
                property 'info'             ,'info'
                property 'format'           ,'format'
                property 'variantValue'     ,'variants'

                property 'assay.id'         ,'assayId'

                property 'geneName'         ,'geneName'
            }

            order 'chr',  'asc'
            order 'pos',  'asc'
            order 'rsId', 'asc'
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
                inSameGroup:           { a, b -> a[0].chr == b[0].chr && a[0].pos == b[0].pos && a[0].rsId == b[0].rsId },
                finalizeGroup:         { List list -> /* list of all the results belonging to a group defined by inSameGroup */
                    /* list of arrays with one element: a map */
                    /* we may have nulls if allowMissingAssays is true,
                     *, but we're guaranteed to have at least one non-null */
                    def firstNonNullCell = list.find()
                    new VcfDataRow(
                            datasetId: firstNonNullCell[0].dataset_id,
                            
                            // Chromosome to define the position
                            chromosome: firstNonNullCell[0].chr,
                            position: firstNonNullCell[0].pos,
                            rsId: firstNonNullCell[0].rsId,

                            // Reference and alternatives for this position
                            referenceAllele: firstNonNullCell[0].ref,
                            alternatives: firstNonNullCell[0].alt,
                            reference: firstNonNullCell[0].reference,

                            // Study level properties
                            quality: firstNonNullCell[0].quality,
                            filter: firstNonNullCell[0].filter,
                            info:  firstNonNullCell[0].info,
                            format: firstNonNullCell[0].format,
                            variants: firstNonNullCell[0].variants,

                            geneName: firstNonNullCell[0].geneName,

                            assayIndexMap: assayIndexMap,
                            data: list.collect {
                                projection.doWithResult it?.getAt(0)
                            }
                    )
                }
        )
    }
}
