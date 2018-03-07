package org.transmartproject.core.userquery

import groovy.transform.CompileStatic

/**
 * Representation of changes made in the query_set - added and removed objects,
 * in comparison to the previous query_set related to the same query
 */
@CompileStatic
class UserQuerySetChangesRepresentation {
    Long id
    SetType setType
    Long setSize
    Date createDate
    String queryName
    Long queryId
    List<String> objectsAdded
    List<String> objectsRemoved
}
