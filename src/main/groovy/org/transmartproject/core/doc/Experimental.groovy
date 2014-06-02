package org.transmartproject.core.doc

import java.lang.annotation.*

/**
 * Annotates experimental features.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE, ElementType.METHOD])
public @interface Experimental { }
