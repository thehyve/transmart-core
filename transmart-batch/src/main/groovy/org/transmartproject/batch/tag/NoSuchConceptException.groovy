package org.transmartproject.batch.tag

import groovy.transform.InheritConstructors
import org.springframework.batch.item.validator.ValidationException

/**
 * A tag referred to in a concept does not exist.
 * Not that this is a Spring BATCH validation exception
 */
@InheritConstructors
class NoSuchConceptException extends ValidationException {
}
