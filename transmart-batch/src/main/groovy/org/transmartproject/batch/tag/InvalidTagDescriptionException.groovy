package org.transmartproject.batch.tag

import groovy.transform.InheritConstructors
import org.springframework.batch.item.validator.ValidationException

/**
 * A tag description is not in the set of allowed values for the tag type.
 * Note that this is a Spring BATCH validation exception
 */
@InheritConstructors
class InvalidTagDescriptionException extends ValidationException {
}
