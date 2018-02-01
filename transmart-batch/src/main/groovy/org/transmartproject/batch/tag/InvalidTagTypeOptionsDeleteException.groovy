package org.transmartproject.batch.tag

import groovy.transform.InheritConstructors
import org.springframework.batch.item.validator.ValidationException

/**
 * Deleting a tag type option fails, because there are still references to the option.
 */
@InheritConstructors
class InvalidTagTypeOptionsDeleteException extends ValidationException {
}
