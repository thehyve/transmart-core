package org.transmartproject.core.users

/**
 * Represents a tranSMART user.
 */
interface User {

    /**
     * The name for the user. Has to be unique in the system.
     *
     * In case of the basic local identity provider this field is stored in the database.
     * In case of Open ID Connect it's the `sub` field.
     *  Note not `username` or `preferred_username` as it does not identify a user.
     *  See http://openid.net/specs/openid-connect-core-1_0.html
     * @return the local username
     */
    String getUsername()

    /**
     * The full real name of the person associated with this entity.
     *
     * @return the user real name
     */
    String getRealName()

    /**
     * The email for the user
     *
     * @return the user email
     */
    String getEmail()

    /**
     * @return true if user is admin, otherwise false
     */
    boolean isAdmin()

    /**
     * The key of the map is a study token. The study token is a string that give access to the study.
     * Level of access specified by the value part of the map {@link AccessLevel}.
     */
    Map<String, AccessLevel> getStudyTokenToAccessLevel()
}
