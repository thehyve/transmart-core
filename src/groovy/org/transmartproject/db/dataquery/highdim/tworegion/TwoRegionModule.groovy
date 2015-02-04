package org.transmartproject.db.dataquery.highdim.tworegion

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

    final Map<String, Class> rowProperties = typesMap(JunctionsRow,
            ['junction'])


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
            createAlias 'junctionEvents', 'junctionEvents', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN
            createAlias 'junctionEvents.event', 'event', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN
            createAlias 'junctionEvents.event.eventGenes', 'eventGenes', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN
            createAlias 'assay', 'assay', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN

            order 'assay.id', 'asc' // important
            order 'id', 'asc' // important

            // because we're using this transformer, every column has to have an alias
            instance.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
        }

        criteriaBuilder
    }

    @Override
    TabularResult transformResults(ScrollableResults results, List<AssayColumn> assays, Projection projection) {

        Map assayIndexMap = createAssayIndexMap assays

        new MultiTabularResult(
                rowsDimensionLabel: 'Regions',
                columnsDimensionLabel: 'Sample codes',
                indicesList: assays,
                results: results,
                inSameGroup: {a, b -> a.junction.id == b.junction.id},
                allowMissingColumns: false,
                finalizeGroup: {List list ->
                    for (def o in list) {
                        Set junctionEvents = list.findAll({
                            it.junctionEvents[0] != null && it.junctionEvents[0].junction.id == o.junction[0].id
                        }).collect({it.junctionEvents[0]}).toSet()
                        o.junction[0].junctionEvents = junctionEvents
                        for (DeTwoRegionJunctionEvent je in junctionEvents) {
                            je.event = list.find({
                                it.event[0].id == je.event.id
                            }).event[0]
                            je.event.eventGenes = list.findAll({
                                it.eventGenes[0] != null && it.eventGenes[0].event.id == je.event.id
                            }).collect({it.eventGenes[0]})
                        }
                    }
                    def js = new JunctionsRow(assayIndexMap, list[0].junction[0])
                    js
                }
        )
    }
}
