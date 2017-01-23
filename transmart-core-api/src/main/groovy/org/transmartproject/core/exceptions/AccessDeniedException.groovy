package org.transmartproject.core.exceptions

import groovy.transform.InheritConstructors

/**
 * Exception to be thrown whenever the user is denied access to some resource.
 */
@InheritConstructors
class AccessDeniedException extends RuntimeException { }
