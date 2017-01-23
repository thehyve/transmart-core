package org.transmartproject.batch.batchartifacts

import groovy.transform.EqualsAndHashCode

/**
 * Used to specify validation errors. See {@link MessageResolverSpringValidator}
 */
@EqualsAndHashCode
class ValidationErrorMatcherBean {
    String field
    String code
}
