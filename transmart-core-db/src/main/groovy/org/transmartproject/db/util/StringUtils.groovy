package org.transmartproject.db.util

import groovy.transform.CompileStatic

@CompileStatic
class StringUtils {

    static final String asLikeLiteral(String s) {
        s.replaceAll(/[\\%_]/, '\\\\$0')
    }

}
