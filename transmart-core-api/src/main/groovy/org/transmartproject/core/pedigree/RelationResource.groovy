package org.transmartproject.core.pedigree

import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.User

interface RelationResource {

    /**
     * Returns all relations in the system.
     *
     * @param user
     * @return all relations
     * @throws AccessDeniedException if user is not an admin or there are some private studies in the database
     */
    List<Relation> getAll(User user) throws AccessDeniedException

}
