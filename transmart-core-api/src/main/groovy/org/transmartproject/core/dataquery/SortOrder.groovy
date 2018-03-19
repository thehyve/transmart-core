package org.transmartproject.core.dataquery

enum SortOrder {
    ASC,
    DESC

    String string() {
        name().toLowerCase()
    }
}