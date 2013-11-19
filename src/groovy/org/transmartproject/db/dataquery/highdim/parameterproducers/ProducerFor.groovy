package org.transmartproject.db.dataquery.highdim.parameterproducers

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to apply to methods of {@link org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory}
 * subtypes, for indicating the name of the constraint or projection that
 * that method handles.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target([ ElementType.METHOD ])
@interface ProducerFor {

    /**
     * The name of the constraint or projection name targeted by the annotated
     * method.
     * @return a constraint/projection name
     */
    String value()
}
