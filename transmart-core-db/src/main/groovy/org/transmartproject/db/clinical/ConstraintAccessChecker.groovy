package org.transmartproject.db.clinical

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.query.BiomarkerConstraint
import org.transmartproject.core.multidimquery.query.Combination
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.ConstraintBuilder
import org.transmartproject.core.multidimquery.query.FieldConstraint
import org.transmartproject.core.multidimquery.query.ModifierConstraint
import org.transmartproject.core.multidimquery.query.MultipleSubSelectionsConstraint
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.NullConstraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.RelationConstraint
import org.transmartproject.core.multidimquery.query.RowValueConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.StudyObjectConstraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.multidimquery.query.TemporalConstraint
import org.transmartproject.core.multidimquery.query.TimeConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.multidimquery.query.ValueConstraint
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.multidimquery.query.InvalidQueryException
import org.transmartproject.db.querytool.QtQueryResultInstance

/**
 * Checks if the user has sufficient permissions to execute a query for a constraint.
 * For constraints that refer to studies, concepts and patient sets, it is checked
 * if the user has sufficient level of access to the study, to a study associated with
 * the concept, or is the owner of the patient set, respectively.
 */
@CompileStatic
class ConstraintAccessChecker extends ConstraintBuilder<Void> {

    AuthorisationChecks authorisationChecks
    // FIXME: Get rid of the legacy checks in this class.
    LegacyAuthorisationChecks legacyAuthorisationChecks

    User user
    PatientDataAccessLevel requiredAccessLevel

    ConstraintAccessChecker(
            User user,
            PatientDataAccessLevel requiredAccessLevel,
            AuthorisationChecks authorisationChecks,
            LegacyAuthorisationChecks legacyAuthorisationChecks
    ) {
        this.user = user
        this.requiredAccessLevel = requiredAccessLevel
        this.authorisationChecks = authorisationChecks
        this.legacyAuthorisationChecks = legacyAuthorisationChecks
    }


    @Override
    Void build(TrueConstraint constraint) { }

    @Override
    Void build(Negation constraint) {
        build(constraint.arg)
    }

    @Override
    Void build(Combination constraint) {
        for (def arg: constraint.args) {
            build(arg)
        }
    }

    @Override
    Void build(SubSelectionConstraint constraint) {
        build(constraint.constraint)
    }

    @Override
    Void build(MultipleSubSelectionsConstraint constraint) {
        for (def arg: constraint.args) {
            build(arg)
        }
    }

    @Override
    Void build(NullConstraint constraint) { }

    @Override
    Void build(BiomarkerConstraint constraint) { }

    @Override
    Void build(ModifierConstraint constraint) { }

    @Override
    Void build(FieldConstraint constraint) {
        if (constraint.field.dimension == 'concept') {
            throw new AccessDeniedException("Access denied. Concept dimension not allowed in field constraints. Use a ConceptConstraint instead.")
        } else if (constraint.field.dimension == 'study') {
            throw new AccessDeniedException("Access denied. Study dimension not allowed in field constraints. Use a StudyConstraint instead.")
        } else if (constraint.field.dimension == 'trial visit') {
            if (constraint.field.fieldName == 'study') {
                throw new AccessDeniedException("Access denied. Field 'study' of trial visit dimension not allowed in field constraints. Use a StudyConstraint instead.")
            }
        }
    }

    @Override
    Void build(ValueConstraint constraint) { }

    @Override
    Void build(RowValueConstraint constraint) { }

    @Override
    Void build(TimeConstraint constraint) { }

    @Override
    @CompileDynamic
    Void build(PatientSetConstraint constraint) {
        if (constraint.patientSetId) {
            QueryResult queryResult = QtQueryResultInstance.findById(constraint.patientSetId)
            if (queryResult == null || !legacyAuthorisationChecks.hasAccess(user, queryResult)) {
                throw new AccessDeniedException("Access denied to patient set or patient set does not exist: ${constraint.patientSetId}")
            }
        }
    }

    @Override
    Void build(TemporalConstraint constraint) {
        build(constraint.eventConstraint)
    }

    @Override
    Void build(ConceptConstraint constraint) {
        constraint = (ConceptConstraint)constraint
        if (constraint.conceptCode && (constraint.conceptCodes || constraint.path) ||
                (constraint.conceptCodes && constraint.path)) {
            throw new InvalidQueryException("Expected one of path and conceptCode(s), got both.")
        } else if (!constraint.conceptCode && !constraint.conceptCodes && !constraint.path) {
            throw new InvalidQueryException("Expected one of path and conceptCode(s), got none.")
        } else if (constraint.conceptCode) {
            if (!legacyAuthorisationChecks.canAccessConceptByCode(user, requiredAccessLevel, constraint.conceptCode)) {
                throw new AccessDeniedException("Access denied to concept code: ${constraint.conceptCode}")
            }
        } else if (constraint.conceptCodes) {
            for (String conceptCode: constraint.conceptCodes) {
                if (!legacyAuthorisationChecks.canAccessConceptByCode(user, requiredAccessLevel, conceptCode)) {
                    throw new AccessDeniedException("Access denied to concept code: ${conceptCode}")
                }
            }
        } else {
            if (!legacyAuthorisationChecks.canAccessConceptByPath(user, requiredAccessLevel, constraint.path)) {
                throw new AccessDeniedException("Access denied to concept path: ${constraint.path}")
            }
        }
    }

    @Override
    @CompileDynamic
    Void build(StudyNameConstraint constraint) {
        def study = Study.findByStudyId(constraint.studyId) as MDStudy
        if (study == null || !authorisationChecks.canReadPatientData(user, requiredAccessLevel, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${constraint.studyId}")
        }
    }

    @Override
    Void build(StudyObjectConstraint constraint) {
        if (constraint.study == null || !authorisationChecks.canReadPatientData(user, requiredAccessLevel, constraint.study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${constraint.study?.name}")
        }
    }

    @Override
    Void build(RelationConstraint constraint) {
        if (constraint.relatedSubjectsConstraint) {
            build(constraint.relatedSubjectsConstraint)
        }
    }

}
