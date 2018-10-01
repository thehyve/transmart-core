package org.transmartproject.db.clinical

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.transmartproject.core.concept.Concept
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
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
    ConceptsResource conceptsResource

    User user
    PatientDataAccessLevel requiredAccessLevel

    ConstraintAccessChecker(
            User user,
            PatientDataAccessLevel requiredAccessLevel,
            AuthorisationChecks authorisationChecks,
            LegacyAuthorisationChecks legacyAuthorisationChecks,
            ConceptsResource conceptsResource
    ) {
        this.user = user
        this.requiredAccessLevel = requiredAccessLevel
        this.authorisationChecks = authorisationChecks
        this.legacyAuthorisationChecks = legacyAuthorisationChecks
        this.conceptsResource = conceptsResource
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
    Void build(BiomarkerConstraint constraint) {
        throw new InvalidQueryException("Not supported yet: ${constraint?.class?.simpleName}.")
    }

    @Override
    Void build(ModifierConstraint constraint) { }

    @Override
    Void build(FieldConstraint constraint) { }

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
                throw new AccessDeniedException("Access denied for ${user.username} user to patient set or patient set does not exist: ${constraint.patientSetId}")
            }
        }
    }

    @Override
    Void build(TemporalConstraint constraint) {
        build(constraint.eventConstraint)
    }

    @Override
    Void build(ConceptConstraint constraint) {
        Concept concept
        if (constraint.path) {
            try {
                concept = conceptsResource.getConceptByConceptPath(constraint.path)
            } catch (NoSuchResourceException e) {
                throw new AccessDeniedException("Access denied for ${user.username} user to concept path: ${constraint.path}")
            }
            if (!legacyAuthorisationChecks.canAccessConcept(user, requiredAccessLevel, concept)) {
                throw new AccessDeniedException("Access denied for ${user.username} user to concept path: ${constraint.path}")
            }
        } else if (constraint.conceptCodes) {
            for (String conceptCode: constraint.conceptCodes) {
                try {
                    concept = conceptsResource.getConceptByConceptCode(conceptCode)
                } catch (NoSuchResourceException e) {
                    throw new AccessDeniedException("Access denied for ${user.username} user to concept code: ${conceptCode}")
                }
                if (!legacyAuthorisationChecks.canAccessConcept(user, requiredAccessLevel, concept)) {
                    throw new AccessDeniedException("Access denied for ${user.username} user to concept code: ${conceptCode}")
                }
            }
        } else {
            try {
                concept = conceptsResource.getConceptByConceptCode(constraint.conceptCode)
            } catch (NoSuchResourceException e) {
                throw new AccessDeniedException("Access denied for ${user.username} user to concept code: ${constraint.conceptCode}")
            }
            if (!legacyAuthorisationChecks.canAccessConcept(user, requiredAccessLevel, concept)) {
                throw new AccessDeniedException("Access denied for ${user.username} user to concept code: ${constraint.conceptCode}")
            }
        }
    }

    @Override
    @CompileDynamic
    Void build(StudyNameConstraint constraint) {
        def study = Study.findByStudyId(constraint.studyId) as MDStudy
        if (study == null || !authorisationChecks.canReadPatientData(user, requiredAccessLevel, study)) {
            throw new AccessDeniedException("Access denied for ${user.username} user to study or study does not exist: ${constraint.studyId}")
        }
    }

    @Override
    Void build(StudyObjectConstraint constraint) {
        if (constraint.study == null || !authorisationChecks.canReadPatientData(user, requiredAccessLevel, constraint.study)) {
            throw new AccessDeniedException("Access denied for ${user.username} user to study or study does not exist: ${constraint.study?.name}")
        }
    }

    @Override
    Void build(RelationConstraint constraint) {
        if (constraint.relatedSubjectsConstraint) {
            build(constraint.relatedSubjectsConstraint)
        }
    }

}
