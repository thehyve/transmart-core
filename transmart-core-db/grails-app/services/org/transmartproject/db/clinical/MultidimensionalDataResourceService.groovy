package org.transmartproject.db.clinical

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
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.util.GormWorkarounds

class MultidimensionalDataResourceService {

    @Autowired
    SessionFactory sessionFactory

    static final int NUM_FIXED_PROJECTIONS = 3

    Hypercube doQuery(Map args) {
        Map constraints = args.constraints
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        def studynames = constraints?.study
        if (studynames instanceof String) studynames = [studynames]
        if(!studynames[0]) {
            throw new RuntimeException("no study provided")
        }

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

            //instance.resultTransformer = Transformers.ALIAS_TO_ENTITY_MAP

            trialVisit {
                study {
                    inList 'studyId', studynames
                }
            }
        }

        // This throws a LegacyStudyException for non-17.1 style studies
        List<Dimension> dimensions = Study.findAll {
            studyId in studynames
        }*.dimensions.flatten()*.dimension.unique()

        Query query = new Query(q, [modifierCodes: ['@']])

        dimensions.each {
            it.selectIDs(query)
        }
        if (query.params.modifierCodes != ['@']) throw new NotImplementedException("Modifer dimensions are not yet implemented")

        q.with {
            inList 'modifierCd', query.params.modifierCodes
            // todo: order by primary-key-except-modifierCodes
        }

        String[] aliases = ((query.criteria.instance as CriteriaImpl).projection as ProjectionList).aliases
        ScrollableResults results = query.criteria.instance.scroll(ScrollMode.FORWARD_ONLY)

        new Hypercube(results, dimensions, aliases, query, session)
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
