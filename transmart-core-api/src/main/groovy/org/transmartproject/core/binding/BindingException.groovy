/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.binding

import groovy.transform.InheritConstructors
import org.transmartproject.core.exceptions.InvalidArgumentsException

import javax.validation.ConstraintViolation

@InheritConstructors
class BindingException<T> extends InvalidArgumentsException {
    final Set<ConstraintViolation<T>> errors

    BindingException(String message, Set<ConstraintViolation<T>> errors) {
        super(message)
        this.errors = errors
    }
}
