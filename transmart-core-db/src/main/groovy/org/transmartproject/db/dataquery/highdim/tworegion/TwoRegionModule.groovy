/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.dataquery.highdim.tworegion

import com.google.common.collect.ImmutableMap
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.hibernate.engine.spi.SessionImplementor
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

    @CompileStatic
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
                allowMissingColumns: false
            ) {
            @Override
            boolean inSameGroup(a, b) { a.junction.id == b.junction.id }

            private void addIf(List list, e) {
                if (e) list.add(e)
            }

            @Override
            JunctionRow finalizeGroup(List<Object[]> list /* list of arrays with one element: a map */) {
                // should be the same in all rows:
                DeTwoRegionJunction junction = (DeTwoRegionJunction) list[0].junction[0]

                List<DeTwoRegionJunctionEvent> allJunctionEvents = []
                for (Object[] e : list) {
                    addIf(allJunctionEvents, e.junctionEvents[0])
                }

                List<DeTwoRegionEvent> allEvents = []
                for (Object[] e : list) {
                    addIf(allEvents, e.event[0])
                }

                List<DeTwoRegionEventGene> allEventGenes = []
                for (Object[] e : list) {
                    addIf(allEventGenes, e.eventGenes[0])
                }

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

                Long assayId = list.first().assay[0].id
                new JunctionRow(assayIdToAssayColumn[assayId],
                        assayIdToAssayIndex[assayId],
                        assays.size(),
                        junction)
            }
        }
    }
}
