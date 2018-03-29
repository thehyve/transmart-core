/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.dataquery.highdim.rnaseq.transcript

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult
import org.transmartproject.db.dataquery.highdim.PlatformImpl
import org.transmartproject.db.dataquery.highdim.chromoregion.RegionRowImpl
import org.transmartproject.db.dataquery.highdim.correlations.CorrelationTypesRegistry
import org.transmartproject.db.dataquery.highdim.correlations.SearchKeywordDataConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.SimpleRealProjectionsFactory
import org.transmartproject.db.dataquery.highdim.rnaseq.RnaSeqValuesProjection

/**
 * Created by olafmeuwese on 14/10/16.
 */
class RnaSeqTranscriptModule extends AbstractHighDimensionDataTypeModule {

    static final String RNASEQ_VALUES_PROJECTION = 'rnaseq_values'

    final List<String> platformMarkerTypes = ['RNASEQ_TRANSCRIPT']

    final String name = 'rnaseq_transcript'

    final String description = "Transcript Level Messenger RNA data (Sequencing)"

    final Map<String, Class> dataProperties = typesMap(DeRnaseqTranscriptData,
            ['readcount', 'normalizedReadcount', 'logNormalizedReadcount', 'zscore'])
    //FIXME Create a transcript entity in the api so the RegionRowImp can be replaced
    final Map<String, Class> rowProperties = typesMap(RegionRowImpl,
            ['id', 'chromosome', 'start', 'end', 'platform', 'bioMarker'])

    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    DataRetrievalParameterFactory chromosomeSegmentConstraintFactory

    @Autowired
    CorrelationTypesRegistry correlationTypesRegistry

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [standardAssayConstraintFactory]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        [
                standardDataConstraintFactory,
                chromosomeSegmentConstraintFactory,
                new SearchKeywordDataConstraintFactory(correlationTypesRegistry,
                        'TRANSCRIPT', 'transcript', 'transcript')
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
                new SimpleRealProjectionsFactory(
                        (Projection.LOG_INTENSITY_PROJECTION): 'logNormalizedReadcount',
                        (Projection.DEFAULT_REAL_PROJECTION):  'normalizedReadcount',
                        (Projection.ZSCORE_PROJECTION):        'zscore'
                ),
                new AllDataProjectionFactory(dataProperties, rowProperties)
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder = createCriteriaBuilder(
                DeRnaseqTranscriptData, 'rnaseqtranscript', session)
        criteriaBuilder.with {
            createAlias('transcript', 'transcript')
            createAlias('transcript.platform', 'platform')

            projections {
                property 'assay.id', 'assayId'
                property 'readcount', 'readcount'
                property 'normalizedReadcount', 'normalizedReadcount'
                property 'logNormalizedReadcount', 'logNormalizedReadcount'
                property 'zscore', 'zscore'

                property 'transcript.transcript', 'transcript'
                property 'transcript.chromosome', 'chromosome'
                property 'transcript.start', 'start'
                property 'transcript.end', 'end'
                property 'transcript.id', 'id'

                property 'platform.id', 'platformId'
                property 'platform.markerType', 'platformMarkerType'
                property 'platform.genomeReleaseId', 'platformGenomeReleaseId'

            }
            order 'transcript.id', 'asc'
            order 'assay.id',  'asc' // important
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)

        }

        criteriaBuilder
    }

    @Override @CompileStatic
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {

        Map assayIndexMap = createAssayIndexMap(assays)

        new DefaultHighDimensionTabularResult<RegionRowImpl>(
                rowsDimensionLabel: "Transcripts",
                columnsDimensionLabel: "Sample codes",
                indicesList: assays,
                results: results,
                allowMissingAssays: true,
            ) {
                @Override @CompileStatic
                def assayIdFromRow(Map row) { row.assayId }

                @Override @CompileStatic
                boolean inSameGroup(Map a, Map b) { a.transcript == b.transcript }

                @Override @CompileStatic
                RegionRowImpl finalizeRow(List<Map> list) {
                    Map firstRow = findFirst list
                    new RegionRowImpl(
                            id: (Long) firstRow.id,
                            chromosome: (String) firstRow.chromosome,
                            start: (Long) firstRow.start,
                            end: (Long) firstRow.end,
                            bioMarker: (String) firstRow.transcript,
                            platform: new PlatformImpl(
                                    id: (String) firstRow.platformId,
                                    markerType: (String) firstRow.platformMarkerType,
                                    genomeReleaseId: (String) firstRow.platformGenomeReleaseId

                            ),
                            assayIndexMap: assayIndexMap,
                            data: doWithProjection(projection, list)
                    )
                }
        }
    }
}
