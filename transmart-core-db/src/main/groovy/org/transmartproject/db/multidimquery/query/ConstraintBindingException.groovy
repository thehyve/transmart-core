/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery.query

import groovy.transform.InheritConstructors
import org.springframework.validation.Errors
import org.transmartproject.core.exceptions.InvalidArgumentsException

@InheritConstructors
class ConstraintBindingException extends InvalidArgumentsException {
    Errors errors

    ConstraintBindingException(String message, Errors errors) {
        super(message)
        this.errors = errors
    }
}
