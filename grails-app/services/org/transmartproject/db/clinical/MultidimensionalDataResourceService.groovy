package org.transmartproject.db.clinical

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.TupleConstructor
import org.apache.commons.lang.NotImplementedException
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.SessionFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.internal.CriteriaImpl
import org.hibernate.internal.StatelessSessionImpl
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.multidimquery.AssayDimension
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.HypercubeImpl
import org.transmartproject.db.multidimquery.ProjectionDimension
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.VisitDimension
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.util.GormWorkarounds

import static org.transmartproject.db.metadata.DimensionDescription.dimensionsMap

class MultidimensionalDataResourceService implements MultiDimensionalDataResource {

    @Autowired
    SessionFactory sessionFactory

    Dimension getDimension(String name) {
        DimensionDescription.findByName(name).dimension
    }

    /**
     * @param accessibleStudies: The studies the current user has access to.
     * @param dataType: The string identifying the data type. "clinical" for clinical data, for high dimensional data
     * the appropriate identifier string (hdd is not yet implemented).
     * @param constraints: (nullable) A list of Constraint-s. If null, selects all the data in the database.
     * @param dimensions: (nullable) A list of Dimension-s to select. Only dimensions valid for the selected studies
     * will actually be applied. If null, select all available dimensions.
     *
     * Not yet implemented:
     * @param sort
     * @param pack
     * @param preloadDimensions
     *
     * @return a Hypercube result
     */
    HypercubeImpl retrieveData(Map args, String dataType, Collection<MDStudy> accessibleStudies) {
        if(dataType != "clinical") throw new NotImplementedException("High dimension datatypes are not yet implemented")

        Constraint constraint = args.constraint
        Set<DimensionImpl> dimensions = ImmutableSet.copyOf(
                args.dimensions.collect {
                    if(it instanceof DimensionImpl) {
                        return it
                    }
                    if(it instanceof String) {
                        def dim = dimensionsMap[it] ?: DimensionDescription.findByName(it)?.dimension
                        if(dim == null) throw new InvalidArgumentsException("Unknown dimension: $it")
                        return dim
                    }
                    throw new InvalidArgumentsException("dimension $it is not a valid dimension or dimension name")
                } ?: [])

        // These are not yet implemented
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        // Add any studies that are being selected on
        def studyIds = QueryService.findStudyNameConstraints(constraint)*.studyId
        Set studies = (studyIds.empty ? [] : Study.findAllByStudyIdInList(studyIds)) +
                QueryService.findStudyObjectConstraints(constraint)*.study as Set

        // We need methods from different interfaces that StatelessSessionImpl implements.
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        session.connection().autoCommit = false

        HibernateCriteriaBuilder q = GormWorkarounds.createCriteriaBuilder(ObservationFact, 'observation_fact', session)
        q.with {
            // The main reason to use this projections block is that it clears all the default projections that
            // select all fields.
            projections {
                // NUM_FIXED_PROJECTIONS must match the number of projections defined here
                property 'valueType', 'valueType'
                property 'textValue', 'textValue'
                property 'numberValue', 'numberValue'
            }
        }

        Set<DimensionImpl> validDimensions

        //TODO Remove after adding all the dimension, added to prevent e2e tests failing
        def notImplementedDimensions = [AssayDimension, BioMarkerDimension, ProjectionDimension]
        if(studies) {
            // This throws a LegacyStudyException for non-17.1 style studies
            // This could probably be done more efficiently, but GORM support for many-to-many collections is pretty
            // buggy. And usually the studies and dimensions will be cached in memory.
            validDimensions = ImmutableSet.copyOf((Set<DimensionImpl>) studies*.dimensions.flatten())

        } else {
            validDimensions = ImmutableSet.copyOf DimensionDescription.all*.dimension.findAll{
                !(it.class in notImplementedDimensions)
            }
        }
        // only allow valid dimensions
        dimensions = (Set<DimensionImpl>) dimensions?.findAll { it in validDimensions } ?: validDimensions

        Query query = new Query(q, [modifierCodes: ['@']])

        dimensions.each {
            it.selectIDs(query)
        }
        if (query.params.modifierCodes != ['@']) {
            if(sort != null) throw new NotImplementedException("sorting is not implemented")

            // Make sure all primary key dimension columns are selected, even if they are not part of the result
            primaryKeyDimensions.each {
                if(!(it in dimensions)) {
                    it.selectIDs(query)
                }
            }

            q.with {
                // instanceNum is not a dimension
                property 'instanceNum', 'instanceNum'

                // TODO: The order of sorting should match the one of the main index (or any index). Todo: create
                // main index.
                // 'modifierCd' needs to be excluded or listed last when using modifiers
                order 'conceptCode'
                order 'providerId'
                order 'patient'
                order 'encounterNum'
                order 'startDate'
                order 'instanceNum'
            }
        }

        q.with {
            inList 'modifierCd', query.params.modifierCodes
            // TODO: order by primary-key-except-modifierCodes
        }

        CriteriaImpl hibernateCriteria = query.criteria.instance
        String[] aliases = (hibernateCriteria.projection as ProjectionList).aliases

        HibernateCriteriaQueryBuilder restrictionsBuilder = new HibernateCriteriaQueryBuilder(
                studies: accessibleStudies
        )
        // TODO: check that aliases set by dimensions and by restrictions don't clash

        restrictionsBuilder.applyToCriteria(hibernateCriteria, [constraint])

        ScrollableResults results = query.criteria.instance.scroll(ScrollMode.FORWARD_ONLY)

        new HypercubeImpl(results, dimensions, aliases, query, session)
        // session will be closed by the Hypercube
    }

    static final List<DimensionImpl> primaryKeyDimensions = ImmutableList.of(
            // primary key columns excluding modifierCd and instanceNum
            dimensionsMap.concept,
            dimensionsMap.provider,
            dimensionsMap.patient,
            dimensionsMap.visit,
            dimensionsMap.'start time',
    )

    /*
    note: efficiently extracting the available dimension elements for dimensions is possible using nonstandard
    sql. For Postgres:
    SELECT array_agg(DISTINCT patient_num), array_agg(DISTINCT concept_cd),
        array_agg(distinct case when modifier_cd = 'SOMEMODIFIER' then tval_char end)... FROM observation_facts WHERE ...
      Maybe doing something with unnest() can also help but I haven't figured that out yet.
    For Oracle we should look into the UNPIVOT operator
     */

}

@TupleConstructor
class Query {
    HibernateCriteriaBuilder criteria
    Map params
}
