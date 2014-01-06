package org.transmartproject.core.doc

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotates experimental features.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE, ElementType.METHOD])
public @interface Experimental { }
