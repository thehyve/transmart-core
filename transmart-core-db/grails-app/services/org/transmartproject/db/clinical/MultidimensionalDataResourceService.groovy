package org.transmartproject.db.clinical

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
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.HypercubeImpl
import org.transmartproject.db.multidimquery.QueryService
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.util.GormWorkarounds

class MultidimensionalDataResourceService implements MultiDimensionalDataResource {

    @Autowired
    SessionFactory sessionFactory

    Dimension getDimension(String name) {
        DimensionDescription.findByName(name).dimension
    }

    /**
     *
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
    HypercubeImpl retrieveData(Map args, String dataType) {
        if(dataType != "clinical") throw new NotImplementedException("High dimension datatypes are not yet implemented")

        Constraint constraint = args.constraint
        Set<DimensionImpl> dimensions = ImmutableSet.copyOf(args.dimensions ?: []) // make unique

        // These are not yet implemented
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        // Add any studies that are being selected on
        Set studies = Study.findAllByStudyIdInList(QueryService.findStudyConstraints(constraint)*.studyId) +
                QueryService.findStudyObjectConstraints(constraint)*.study as Set

        // We need methods from different interfaces that StatelessSessionImpl implements.
        def session = (StatelessSessionImpl) sessionFactory.openStatelessSession()
        session.connection().autoCommit = false

        HibernateCriteriaBuilder q = GormWorkarounds.createCriteriaBuilder(ObservationFact, 'observations', session)
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
        if(studies) {
            // This throws a LegacyStudyException for non-17.1 style studies
            // This could probably be done more efficiently, but GORM support for many-to-many collections is pretty
            // buggy. And usually the studies and dimensions will be cached in memory.
            validDimensions = ImmutableSet.copyOf studies*.dimensions.flatten()*.dimension

        } else {
            validDimensions = ImmutableSet.copyOf DimensionDescription.all*.dimension
        }
        // only allow valid dimensions
        dimensions = (Set<DimensionImpl>) dimensions?.findAll { it in validDimensions } ?: validDimensions

        Query query = new Query(q, [modifierCodes: ['@']])

        dimensions.each {
            it.selectIDs(query)
        }
        if (query.params.modifierCodes != ['@']) throw new NotImplementedException("Modifier dimensions are not yet implemented")

        q.with {
            inList 'modifierCd', query.params.modifierCodes
            // TODO: order by primary-key-except-modifierCodes
        }

        CriteriaImpl hibernateCriteria = query.criteria.instance
        String[] aliases = (hibernateCriteria.projection as ProjectionList).aliases

        HibernateCriteriaQueryBuilder restrictionsBuilder = new HibernateCriteriaQueryBuilder()
        // TODO: check that aliases set by dimensions and by restrictions don't clash

        restrictionsBuilder.applyToCriteria(hibernateCriteria, [constraint])

        ScrollableResults results = query.criteria.instance.scroll(ScrollMode.FORWARD_ONLY)

        new HypercubeImpl(results, dimensions, aliases, query, session)
        // session will be closed by the Hypercube
    }

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
