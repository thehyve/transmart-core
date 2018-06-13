package org.transmartproject.core.users

interface AuthorisationChecks {

    /**
     * Checks if a {@link org.transmartproject.core.ontology.MDStudy} (in the i2b2demodata schema)
     * exists to which the user has access.
     *
     * @param user the user to check access for.
     * @param accessLevel level of access
     * @param study the study object that is referred to from the trial visit dimension.
     * @return true iff a study exists that the user has access to.
     */
    boolean canPerform(User user,
                       AccessLevel accessLevel,
                       ProtectedResource protectedResource)

}