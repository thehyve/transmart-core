/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery.query

import groovy.transform.InheritConstructors
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.query.Constraint

import javax.validation.ConstraintViolation

@InheritConstructors
class ConstraintBindingException extends InvalidArgumentsException {
    final Set<ConstraintViolation<Constraint>> errors

    ConstraintBindingException(String message, Set<ConstraintViolation<Constraint>> errors) {
        super(message)
        this.errors = errors
    }
}
