package org.transmartproject.core.users

import groovy.transform.Immutable

/**
 * Simple implementation of {@link User}
 */
@Immutable
class SimpleUser implements User {
    String username
    String realName
    String email
    boolean admin
    Map<String, AccessLevel> studyTokenToAccessLevel
}
