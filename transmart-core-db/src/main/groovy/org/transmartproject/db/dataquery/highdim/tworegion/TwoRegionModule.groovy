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
import org.transmartproject.core.dataquery.assay.Assay
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

        Map<Long, AssayColumn> assayIdToAssayColumn = [:]
        for(def assay : assays) {
            assayIdToAssayColumn[assay.id] = assay
        }
        Map<Long, Integer> assayIdToAssayIndex = [:]
        for(int i=0; i<assays.size(); i++) {
            assayIdToAssayIndex[assays[i].id] = i
        }

        new MultiTabularResult(
                rowsDimensionLabel: 'Regions',
                columnsDimensionLabel: 'Sample codes',
                indicesList: assays,
                results: results,
                allowMissingColumns: false
            ) {
            @Override @CompileStatic
            boolean inSameGroup(Object[] a, Object[] b) {
                ((DeTwoRegionJunction) ((Map) a[0]).junction).id ==
                        ((DeTwoRegionJunction) ((Map) b[0]).junction).id
            }

            @CompileStatic
            private static List addAllIf(List<Object[]> list, String key) {
                List result = []
                for (Object[] e : list) {
                    def map = (Map<String,Object>) e[0]
                    def val = map[key]
                    if(val != null) result.add(val)
                }
                result
            }

            @Override @CompileStatic
            JunctionRow finalizeGroup(List<Object[]> list /* list of arrays with one element: a map */) {
                // should be the same in all rows:
                DeTwoRegionJunction junction = (DeTwoRegionJunction) ((Map) list[0][0]).junction

                List<DeTwoRegionJunctionEvent> allJunctionEvents = addAllIf(list, 'junctionEvents')
                List<DeTwoRegionEvent> allEvents = addAllIf(list, 'event')
                List<DeTwoRegionEventGene> allEventGenes = addAllIf(list, 'eventGenes')

                // Assign junction events to junction
                junction.junctionEvents = allJunctionEvents as Set

                // Assign events to junction events
                outer: for(def je : allJunctionEvents) {
                    for(def event : allEvents) {
                        if (event.id == je.event.id) {
                            je.event = event
                            break outer
                        }
                    }
                }

                // Assign event genes to events
                for(def event : allEvents) {
                    Set eventGenes = new LinkedHashSet()
                    for(def gene : allEventGenes) {
                        if (gene.event.id == event.id) eventGenes.add(gene)
                    }
                    event.eventGenes = eventGenes
                }

                Long assayId = ((Assay) ((Map) list[0][0]).assay).id
                new JunctionRow(assayIdToAssayColumn[assayId],
                        assayIdToAssayIndex[assayId],
                        assays.size(),
                        junction)
            }
        }
    }
}
