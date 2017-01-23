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

package org.transmartproject.db.dataquery.highdim.acgh

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
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.querytool.HighDimensionFilterType
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.PlatformImpl
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.chromoregion.DeChromosomalRegion
import org.transmartproject.db.dataquery.highdim.chromoregion.RegionRowImpl
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleAnnotationConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionsFactory

import static org.hibernate.sql.JoinFragment.INNER_JOIN
import static org.transmartproject.db.util.GormWorkarounds.createCriteriaBuilder

class AcghModule extends AbstractHighDimensionDataTypeModule {

    static final String ACGH_VALUES_PROJECTION = 'acgh_values'

    final List<String> platformMarkerTypes = ['Chromosomal']

    final String name = 'acgh'

    final String description = "ACGH data"

    final Map<String, Class> dataProperties = typesMap(DeSubjectAcghData,
            ['chipCopyNumberValue', 'segmentCopyNumberValue', 'flag',
             'probabilityOfLoss', 'probabilityOfNormal', 'probabilityOfGain', 'probabilityOfAmplification'])

    final Map<String, Class> rowProperties = typesMap(RegionRowImpl,
            ['id', 'name', 'cytoband', 'chromosome', 'start', 'end', 'numberOfProbes', 'bioMarker'])

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    ChromosomeSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    HighDimensionDataTypeResource createHighDimensionResource(Map params) {
        /* return instead subclass of HighDimensionDataTypeResourceImpl,
         * because we add a method, retrieveChromosomalSegments() */
        new AcghDataTypeResource(this)
    }

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [standardAssayConstraintFactory]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [
                standardDataConstraintFactory,
                chromosomeSegmentConstraintFactory,
                new SimpleAnnotationConstraintFactory(field: 'region', annotationClass: DeChromosomalRegion.class),
                new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                        'GENE', 'region', 'geneId')
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {
        [
                new MapBasedParameterFactory(
                        (ACGH_VALUES_PROJECTION): { Map<String, Object> params ->
                            if (!params.isEmpty()) {
                                throw new InvalidArgumentsException('Expected no parameters here')
                            }
                            new AcghValuesProjection()
                        }
                ),
                new SimpleRealProjectionsFactory(
                        (Projection.CHIP_COPYNUMBER_VALUE)       : 'acgh.chipCopyNumberValue',
                        (Projection.SEGMENT_COPY_NUMBER_VALUE)   : 'acgh.segmentCopyNumberValue',
                        (Projection.FLAG)                        : 'acgh.flag',
                        (Projection.PROB_LOSS)                   : 'acgh.probabilityOfLoss',
                        (Projection.PROB_NORM)                   : 'acgh.probabilityOfNormal',
                        (Projection.PROB_GAIN)                   : 'acgh.probabilityOfGain',
                        (Projection.PROB_AMP)                    : 'acgh.probabilityOfAmplification'
                ),
                new AllDataProjectionFactory(dataProperties, rowProperties)
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(DeSubjectAcghData, 'acgh', session)

        criteriaBuilder.with {
            createAlias 'jRegion', 'region', INNER_JOIN
            createAlias 'jRegion.platform', 'platform', INNER_JOIN

            projections {
                property 'acgh.assay.id', 'assayId'
                property 'acgh.chipCopyNumberValue', 'chipCopyNumberValue'
                property 'acgh.segmentCopyNumberValue', 'segmentCopyNumberValue'
                property 'acgh.flag', 'flag'
                property 'acgh.probabilityOfLoss', 'probabilityOfLoss'
                property 'acgh.probabilityOfNormal', 'probabilityOfNormal'
                property 'acgh.probabilityOfGain', 'probabilityOfGain'
                property 'acgh.probabilityOfAmplification', 'probabilityOfAmplification'

                property 'region.id', 'id'
                property 'region.name', 'name'
                property 'region.cytoband', 'cytoband'
                property 'region.chromosome', 'chromosome'
                property 'region.start', 'start'
                property 'region.end', 'end'
                property 'region.numberOfProbes', 'numberOfProbes'
                property 'region.geneSymbol', 'geneSymbol'

                property 'platform.id', 'platformId'
                property 'platform.title', 'platformTitle'
                property 'platform.organism', 'platformOrganism'
                property 'platform.annotationDate', 'platformAnnotationDate'
                property 'platform.markerType', 'platformMarkerType'
                property 'platform.genomeReleaseId', 'platformGenomeReleaseId'
            }

            order 'region.id', 'asc'
            order 'assay.id', 'asc' // important

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
                rowsDimensionLabel: 'Regions',
                columnsDimensionLabel: 'Sample codes',
                indicesList: assays,
                results: results,
                inSameGroup: { a, b -> a.id == b.id },
                finalizeGroup: { List list -> /* list of arrays with 15 elements (1/projection) */
                    if (list.size() != assays.size()) {
                        throw new UnexpectedResultException(
                                "Expected group to be of size ${assays.size()}; got ${list.size()} objects")
                    }
                    def cell = list.find()[0]
                    def regionRow = new RegionRowImpl(
                            id: cell.id,
                            name: cell.name,
                            cytoband: cell.cytoband,
                            chromosome: cell.chromosome,
                            start: cell.start,
                            end: cell.end,
                            numberOfProbes: cell.numberOfProbes,
                            bioMarker: cell.geneSymbol,
                            platform: new PlatformImpl(
                                    id:              cell.platformId,
                                    title:           cell.platformTitle,
                                    organism:        cell.platformOrganism,
                                    //It converts timestamp to date
                                    annotationDate:  cell.platformAnnotationDate ?
                                            new Date(cell.platformAnnotationDate.getTime())
                                            : null,
                                    markerType:      cell.platformMarkerType,
                                    genomeReleaseId: cell.platformGenomeReleaseId
                            ),

                            assayIndexMap: assayIndexMap
                    )
                    regionRow.data = list.collect {
                        projection.doWithResult(it?.getAt(0))
                    }
                    regionRow
                }
        )
    }

    @Override
    List<String> searchAnnotation(String concept_code, String search_term, String search_property) {
        if (!getSearchableAnnotationProperties().contains(search_property))
            return []
        DeChromosomalRegion.createCriteria().list {
            eq('gplId', DeSubjectSampleMapping.createCriteria().get {
                eq('conceptCode', concept_code)
                projections {distinct 'platform.id'}
            })
            ilike(search_property, search_term + '%')
            projections { distinct(search_property) }
            order(search_property, 'ASC')
            maxResults(100)
        }
    }

    @Override
    List<String> getSearchableAnnotationProperties() {
        ['geneSymbol', 'cytoband', 'name']
    }

    @Override
    HighDimensionFilterType getHighDimensionFilterType() {
        HighDimensionFilterType.ACGH
    }

    @Override
    List<String> getSearchableProjections() {
        [Projection.CHIP_COPYNUMBER_VALUE, Projection.SEGMENT_COPY_NUMBER_VALUE, Projection.FLAG, Projection.PROB_AMP,
         Projection.PROB_GAIN, Projection.PROB_LOSS, Projection.PROB_NORM]
    }
}
