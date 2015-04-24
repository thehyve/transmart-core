package org.transmartproject.db.dataquery.highdim.tworegion

import com.google.common.collect.ImmutableMap
import grails.orm.HibernateCriteriaBuilder
import org.hibernate.ScrollableResults
import org.hibernate.engine.SessionImplementor
import org.hibernate.transform.Transformers
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.MultiTabularResult
import org.transmartproject.db.dataquery.highdim.AbstractHighDimensionDataTypeModule
import org.transmartproject.db.dataquery.highdim.chromoregion.TwoChromosomesSegmentConstraintFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.AllDataProjectionFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory

import static org.hibernate.criterion.CriteriaSpecification.INNER_JOIN
import static org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN
import static org.transmartproject.db.util.GormWorkarounds.createCriteriaBuilder

/**
 * Created by j.hudecek on 4-12-2014.
 */
class TwoRegionModule extends AbstractHighDimensionDataTypeModule {

    final String name = 'two_region'

    final String description = "Two  Variant data"
    final String TWO_REGION_PROJECTION = "two_region_label"

    final List<String> platformMarkerTypes = ['two_region']

    final Map<String, Class> dataProperties = typesMap(DeTwoRegionJunction,
            ['upChromosome', 'downChromosome', 'id', 'upEnd', 'upPos', 'upStrand', 'downEnd', 'downPos', 'downStrand', 'isInFrame'])

    final Map<String, Class> rowProperties = ImmutableMap.of()


    @Autowired
    DataRetrievalParameterFactory standardAssayConstraintFactory

    @Autowired
    DataRetrievalParameterFactory standardDataConstraintFactory

    @Autowired
    TwoChromosomesSegmentConstraintFactory chromosomeSegmentConstraintFactory

    @Override
    protected List<DataRetrievalParameterFactory> createAssayConstraintFactories() {
        [standardAssayConstraintFactory]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createDataConstraintFactories() {
        chromosomeSegmentConstraintFactory.segmentPrefix = ''
        chromosomeSegmentConstraintFactory.segmentChromosomeColumn = 'upChromosome'
        chromosomeSegmentConstraintFactory.segmentStartColumn = 'upPos'
        chromosomeSegmentConstraintFactory.segmentEndColumn = 'upEnd'
        chromosomeSegmentConstraintFactory.segmentTwoPrefix = ''
        chromosomeSegmentConstraintFactory.segmentTwoChromosomeColumn = 'downChromosome'
        chromosomeSegmentConstraintFactory.segmentTwoStartColumn = 'downPos'
        chromosomeSegmentConstraintFactory.segmentTwoEndColumn = 'downEnd'
        [
                chromosomeSegmentConstraintFactory
        ]
    }

    @Override
    protected List<DataRetrievalParameterFactory> createProjectionFactories() {

        [
                new AllDataProjectionFactory(dataProperties, rowProperties)
        ]
    }

    @Override
    HibernateCriteriaBuilder prepareDataQuery(Projection projection, SessionImplementor session) {
        HibernateCriteriaBuilder criteriaBuilder =
                createCriteriaBuilder(DeTwoRegionJunction, 'junction', session)

        criteriaBuilder.with {
            createAlias 'junctionEvents', 'junctionEvents', LEFT_JOIN
            createAlias 'junctionEvents.event', 'event', LEFT_JOIN
            createAlias 'junctionEvents.event.eventGenes', 'eventGenes', LEFT_JOIN
            createAlias 'assay', 'assay', INNER_JOIN

            order 'id', 'asc' // important
            // no need to order by assay because groups only contain one assay

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {

        Map<Long, AssayColumn> assayIdToAssayColumn = assays.collectEntries {
            [it.id, it]
        }
        int i = 0
        Map<Long, Long> assayIdToAssayIndex = assays.collectEntries {
            [it.id, i++]
        }

        new MultiTabularResult(
                rowsDimensionLabel: 'Regions',
                columnsDimensionLabel: 'Sample codes',
                indicesList: assays,
                results: results,
                inSameGroup: { a, b -> a.junction.id == b.junction.id },
                allowMissingColumns: false,
                finalizeGroup : { List<Map<String, List<Object>>> rows ->
                    // should be the same in all rows:
                    DeTwoRegionJunction junction = rows[0].junction[0]

                    List<DeTwoRegionJunctionEvent> allJunctionEvents = rows
                            .collect { it.junctionEvents[0] }
                            .findAll()

                    List<DeTwoRegionEvent> allEvents = rows
                            .collect { it.event[0] }
                            .findAll()

                    List<DeTwoRegionEventGene> allEventGenes = rows
                            .collect { it.eventGenes[0] }
                            .findAll()

                    // Assign junction events to junction
                    junction.junctionEvents = allJunctionEvents as Set

                    // Assign events to junction events
                    allJunctionEvents.each { DeTwoRegionJunctionEvent je ->
                        je.event = allEvents.find { it.id == je.event.id }
                    }

                    // Assign event genes to events
                    allEvents.each { DeTwoRegionEvent event ->
                        event.eventGenes = allEventGenes
                                .findAll { it.event.id == event.id } as Set
                    }

                    Long assayId = rows.first().assay[0].id
                    new JunctionRow(assayIdToAssayColumn[assayId],
                            assayIdToAssayIndex[assayId],
                            assays.size(),
                            junction)
                }
        )
    }
}
