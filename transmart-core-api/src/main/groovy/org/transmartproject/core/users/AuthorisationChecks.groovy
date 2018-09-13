package org.transmartproject.core.users

import org.transmartproject.core.ontology.MDStudy

/**
 * 17.1 data model (study entity, hypercube, constraints language) authorisation checks.
 */
interface AuthorisationChecks {

    /**
     * Check whether user has to the study patient data of the given level.
     * @param user - user we check read permission for
     * @param requiredAccessLevel - minimal access level user has to have
     * @param study - study we check user permission for
     * @return true if user has access to the data
     */
    boolean canReadPatientData(User user, PatientDataAccessLevel requiredAccessLevel, MDStudy study)

    /**
     * Check whether user has any access to the study
     * @return true if user has access to the study
     */
    boolean hasAnyAccess(User user, MDStudy study)

}
