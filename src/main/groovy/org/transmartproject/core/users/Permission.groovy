package org.transmartproject.core.users

/**
 * Describes a set of operations that can be performed on a certain resource.
 *
 * At this point, the protected resources are only studies, and the only
 * available permissions are:
 *
 * <ul>
 *     <li><code>OWN</code></li>
 *     <li><code>EXPORT</code></li>
 *     <li><code>VIEW</code></li>
 * </ul>
 *
 */
public interface Permission {

    /**
     * The name of the permission. Typically, client code is not interested in
     * the permissions themselves, but instead on the set of operations that
     * a user can perform.
     *
     * Therefore, this type will be more useful for introspection or for
     * assigning permissions to users, which is still not supported by this API.
     *
     * @return the name of this permission
     */
    String getName()

    /**
     * Returns true iif this permission cover the operation described by the
     * argument.
     *
     * This overrides the Groovy <code>in</code> operator
     *
     * @param operation the operation one wants to test for inclusion in this
     * permission
     * @return true iif this permission includes the passed operation
     */
    boolean isCase(ProtectedOperation operation)

}
