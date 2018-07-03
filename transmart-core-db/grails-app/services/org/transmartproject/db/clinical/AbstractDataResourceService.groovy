package org.transmartproject.db.clinical

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.IterableResult
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.util.ScrollableResultsWrappingIterable

@CompileStatic
class AbstractDataResourceService {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    AuthorisationChecks authorisationChecks

    @Autowired
    LegacyAuthorisationChecks legacyAuthorisationChecks

    @Autowired
    ConceptsResource conceptsResource

    @Transactional(readOnly = true)
    protected void checkAccess(Constraint constraint, User user, PatientDataAccessLevel requiredAccessLevel) throws AccessDeniedException {
        if (!user) {
            throw new AccessDeniedException("No user specified")
        }
        if (!constraint) {
            throw new InvalidArgumentsException("No constraint specified")
        }

        new ConstraintAccessChecker(user, requiredAccessLevel,
                authorisationChecks, legacyAuthorisationChecks, conceptsResource)
                .build(constraint)
    }

    protected HibernateCriteriaQueryBuilder getCheckedQueryBuilder(
            User user, PatientDataAccessLevel requiredAccessLevel) {
        if (user.admin) {
            return HibernateCriteriaQueryBuilder.forAllStudies()
        }
        HibernateCriteriaQueryBuilder.forStudies(authorisationChecks.getStudiesForUser(user, requiredAccessLevel))
    }

    protected Object getFirst(DetachedCriteria criteria) {
        getExecutableCriteria(criteria).setMaxResults(1).uniqueResult()
    }

    protected Object get(DetachedCriteria criteria) {
        getExecutableCriteria(criteria).uniqueResult()
    }

    protected List getList(DetachedCriteria criteria) {
        getExecutableCriteria(criteria).list()
    }

    protected IterableResult getIterable(DetachedCriteria criteria) {
        def scrollableResult = getExecutableCriteria(criteria).scroll(ScrollMode.FORWARD_ONLY)
        new ScrollableResultsWrappingIterable(scrollableResult)
    }

    protected Criteria getExecutableCriteria(DetachedCriteria criteria) {
        criteria.getExecutableCriteria(sessionFactory.currentSession).setCacheable(true)
    }

}
