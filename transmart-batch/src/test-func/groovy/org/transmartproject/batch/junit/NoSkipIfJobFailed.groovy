package org.transmartproject.batch.junit

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation understood by {@link SkipIfJobFailedRule} that prevents it from
 * skipping the rest if the job failed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface NoSkipIfJobFailed {
}
