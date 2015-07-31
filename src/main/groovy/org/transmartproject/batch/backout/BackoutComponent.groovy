package org.transmartproject.batch.backout

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation for classes detected as beans for backout jobs.
 * Analogous to {@link org.springframework.stereotype.Component}, which is not
 * used in order to avoid the backout components from being auto-loaded when
 * not runnign backout jobs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface BackoutComponent {

    // the suggested component name
    String value() default ""
}
