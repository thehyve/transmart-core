package org.transmartproject.db.ontology

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.ontology.MDStudiesResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.db.metadata.DimensionDescription
import org.transmartproject.db.user.User as DbUser
import org.transmartproject.db.accesscontrol.AccessControlChecks
import org.transmartproject.db.i2b2data.Study

@Transactional
class MDStudiesService implements MDStudiesResource {

    @Autowired
    UsersResource usersResource

    @Autowired
    AccessControlChecks accessControlChecks

    static private isLegacyStudy(Study study) {
        if (study == null) {
            false
        } else {
            return study.dimensionDescriptions.any { it.name == DimensionDescription.LEGACY_MARKER }
        }
    }

    @Override
    List<MDStudy> getStudies(User currentUser) {
        def user = (DbUser) usersResource.getUserFromUsername(currentUser.username)
        accessControlChecks.getDimensionStudiesForUser(user).findAll { !isLegacyStudy(it) }
    }

    @Override
    MDStudy getStudyForUser(Long id, User currentUser) throws NoSuchResourceException {
        def user = usersResource.getUserFromUsername(currentUser.username)
        def study = Study.findById(id)
        if (isLegacyStudy(study)) {
            study = null
        }
        if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${id}")
        }
        study
    }

    @Override
    MDStudy getStudyByStudyIdForUser(String studyId, User currentUser) throws NoSuchResourceException {
        def user = usersResource.getUserFromUsername(currentUser.username)
        def study = Study.findByStudyId(studyId)
        if (isLegacyStudy(study)) {
            study = null
        }
        if (study == null || !user.canPerform(ProtectedOperation.WellKnownOperations.READ, study)) {
            throw new AccessDeniedException("Access denied to study or study does not exist: ${studyId}")
        }
        study
    }

}
