/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.util

import groovy.transform.CompileStatic
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.LikeExpression
import org.hibernate.criterion.MatchMode

@CompileStatic
class StringUtils {

    static final String asLikeLiteral(String s) {
        s.replaceAll(/[\\%_]/, '\\\\$0')
    }

    static final Criterion like(String propertyName, String value, MatchMode mode) {
        new LikeExpression(propertyName, asLikeLiteral(value), mode, '\\' as char, false) {}
    }

    static final Criterion like(String propertyName, String value) {
        like(propertyName, value, MatchMode.EXACT)
    }

    static final Criterion startsWith(String propertyName, String value) {
        like(propertyName, value, MatchMode.START)
    }

    static final Criterion contains(String propertyName, String value) {
        like(propertyName, value, MatchMode.ANYWHERE)
    }

}
