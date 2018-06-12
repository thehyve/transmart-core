package org.transmartproject.db.clinical

import grails.transaction.Transactional
import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.IterableResult
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.User
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.BioMarkerDimension
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.db.multidimquery.query.HibernateCriteriaQueryBuilder
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.core.multidimquery.query.ModifierConstraint
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.NullConstraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.RelationConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.StudyObjectConstraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.multidimquery.query.TemporalConstraint
import org.transmartproject.core.multidimquery.query.TimeConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.multidimquery.query.ValueConstraint
import org.transmartproject.db.querytool.QtQueryResultInstance
import org.transmartproject.db.util.ScrollableResultsWrappingIterable

import static org.transmartproject.db.multidimquery.DimensionImpl.*

class AbstractDataResourceService {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    AccessControlChecks accessControlChecks

    @Transactional(readOnly = true)
    protected void checkAccess(Constraint constraint, User user) throws AccessDeniedException {
        assert user, 'user is required'
        assert constraint, 'constraint is required'

        if (constraint instanceof TrueConstraint
                || constraint instanceof ModifierConstraint
                || constraint instanceof ValueConstraint
                || constraint instanceof TimeConstraint
                || constraint instanceof NullConstraint
                || constraint instanceof RelationConstraint) {
            //
        } else if (constraint instanceof Negation) {
            checkAccess(constraint.arg, user)
        } else if (constraint instanceof Combination) {
            constraint.args.each { checkAccess(it, user) }
        } else if (constraint instanceof TemporalConstraint) {
            checkAccess(constraint.eventConstraint, user)
        } else if (constraint instanceof SubSelectionConstraint) {
            checkAccess(constraint.constraint, user)
        } else if (constraint instanceof BioMarkerDimension) {
            throw new InvalidQueryException("Not supported yet: ${constraint?.class?.simpleName}.")
        } else if (constraint instanceof PatientSetConstraint) {
            if (constraint.patientSetId) {
                QueryResult queryResult = QtQueryResultInstance.findById(constraint.patientSetId)
                if (queryResult == null || !accessControlChecks.canPerform(user, ProtectedOperation.WellKnownOperations.READ, queryResult)) {
                    throw new AccessDeniedException("Access denied to patient set or patient set does not exist: ${constraint.patientSetId}")
                }
            }
        } else if (constraint instanceof FieldConstraint) {
            if (constraint.field.dimension == CONCEPT.name) {
                throw new AccessDeniedException("Access denied. Concept dimension not allowed in field constraints. Use a ConceptConstraint instead.")
            } else if (constraint.field.dimension == STUDY.name) {
                throw new AccessDeniedException("Access denied. Study dimension not allowed in field constraints. Use a StudyConstraint instead.")
            } else if (constraint.field.dimension == TRIAL_VISIT.name) {
                if (constraint.field.fieldName == 'study') {
                    throw new AccessDeniedException("Access denied. Field 'study' of trial visit dimension not allowed in field constraints. Use a StudyConstraint instead.")
                }
            }
        } else if (constraint instanceof ConceptConstraint) {
            constraint = (ConceptConstraint)constraint
            if (constraint.conceptCode && (constraint.conceptCodes || constraint.path) ||
                    (constraint.conceptCodes && constraint.path)) {
                throw new InvalidQueryException("Expected one of path and conceptCode(s), got both.")
            } else if (!constraint.conceptCode && !constraint.conceptCodes && !constraint.path) {
                throw new InvalidQueryException("Expected one of path and conceptCode(s), got none.")
            } else if (constraint.conceptCode) {
                if (!accessControlChecks.checkConceptAccess(user, conceptCode: constraint.conceptCode)) {
                    throw new AccessDeniedException("Access denied to concept code: ${constraint.conceptCode}")
                }
            } else if (constraint.conceptCodes) {
                for (String conceptCode: constraint.conceptCodes) {
                    if (!accessControlChecks.checkConceptAccess(user, conceptCode: conceptCode)) {
                        throw new AccessDeniedException("Access denied to concept code: ${conceptCode}")
                    }
                }
            } else {
                if (!accessControlChecks.checkConceptAccess(user, conceptPath: constraint.path)) {
                    throw new AccessDeniedException("Access denied to concept path: ${constraint.path}")
                }
            }
        } else if (constraint instanceof StudyNameConstraint) {
            def study = Study.findByStudyId(constraint.studyId)
            def mostLimitedOperation = ProtectedOperation.WellKnownOperations.SHOW_SUMMARY_STATISTICS
            if (study == null || !accessControlChecks.canPerform(user, mostLimitedOperation, study)) {
                throw new AccessDeniedException("Access denied to study or study does not exist: ${constraint.studyId}")
            }
        } else if (constraint instanceof StudyObjectConstraint) {
            def mostLimitedOperation = ProtectedOperation.WellKnownOperations.SHOW_SUMMARY_STATISTICS
            if (constraint.study == null || !accessControlChecks.canPerform(user, mostLimitedOperation, constraint.study)) {
                throw new AccessDeniedException("Access denied to study or study does not exist: ${constraint.study?.name}")
            }
        } else {
            throw new InvalidQueryException("Unknown constraint type: ${constraint?.class?.simpleName}.")
        }
    }

    protected HibernateCriteriaQueryBuilder getCheckedQueryBuilder(User user) {
        def unlimitedStudiesAccess = accessControlChecks.hasUnlimitedStudiesAccess(user)
        unlimitedStudiesAccess ? HibernateCriteriaQueryBuilder.forAllStudies() :
                HibernateCriteriaQueryBuilder.forStudies(accessControlChecks.getDimensionStudiesForUser(user))
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
