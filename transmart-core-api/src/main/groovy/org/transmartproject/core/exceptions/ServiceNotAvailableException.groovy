package org.transmartproject.core.exceptions

import groovy.transform.InheritConstructors

/**
 * Exception to be thrown whenever the requested operation is (temporarily)
 * currently not available.
 */
@InheritConstructors
class ServiceNotAvailableException extends RuntimeException { }
