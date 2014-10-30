package org.transmartproject.batch.startup

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 * Signals that a .params file is invalid or refers to files that don't exist.
 * Thrown in {@link ExternalJobParameters} and subclasses.
 */
@CompileStatic
@InheritConstructors
class InvalidParametersFileException extends Exception { }
