package org.transmartproject.core.exceptions

import groovy.transform.InheritConstructors

/**
 * An exception type to designate the submission of invalid data to a resource
 * method.
 */
@InheritConstructors
class InvalidRequestException extends RuntimeException { }
