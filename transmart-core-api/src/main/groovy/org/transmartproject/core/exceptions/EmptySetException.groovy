package org.transmartproject.core.exceptions

import groovy.transform.InheritConstructors

/**
 * Exception to be thrown whenever some query that should return a non-empty set
 * does return an empty set.
 */
@InheritConstructors
class EmptySetException extends RuntimeException { }
