package org.transmartproject.core.users
/**
 * Represents a tranSMART user.
 */
public interface User {

    /**
     * An numerical identifier for the user.
     *
     * @return numeric unique identifier for the user
     */
    Long getId()

    /**
     * The local name for the user. If the user logs in through tranSMART (as
     * opposed to some SSO solution), this is the username he should input.
     *
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
     * Returns true iif this user is authorized to perform the given operation
     * on the given object.
     *
     * @param operation
     * @param protectedResource
     * @return true iif access is granted
     */
    boolean canPerform(ProtectedOperation operation, ProtectedResource protectedResource)

}
