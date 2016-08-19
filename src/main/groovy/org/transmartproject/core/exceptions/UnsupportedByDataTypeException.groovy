package org.transmartproject.core.exceptions

import groovy.transform.InheritConstructors

/**
 * Exception to be thrown whenever some requested feature is unsupported by the
 * data type being used.
 */
@InheritConstructors
class UnsupportedByDataTypeException extends RuntimeException { }
