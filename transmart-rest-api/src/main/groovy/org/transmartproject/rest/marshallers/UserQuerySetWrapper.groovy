package org.transmartproject.rest.marshallers

class UserQuerySetWrapper {
    Long id
    Long queryId
    String queryName
    String queryUsername
    String setType
    Long setSize
    Date createDate
    Collection<UserQuerySetDiffWrapper> diffs
    Collection<UserQuerySetInstanceWrapper> instances
}
