package org.transmartproject.batch.clinical.ontology

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

/**
 * Expected to find a node, but could not find it.
 */
@CompileStatic
@InheritConstructors
class NodeNotFoundException extends RuntimeException {}
