package org.transmart.server.subsctiption

import groovy.transform.CompileStatic

/**
 * The update data change type
 */
@CompileStatic
enum ChangeFlag {

    ADDED ('ADDED'),
    REMOVED ('REMOVED')

    private String changeFlag

    ChangeFlag(String changeFlag) {
        this.changeFlag = changeFlag
    }

    static ChangeFlag from(String changeFlag) {
        ChangeFlag f = values().find { it.changeFlag == changeFlag }
        if (f == null) throw new Exception("Unknown change flag: ${changeFlag}")
        f
    }

    String value() {
        changeFlag
    }
}
