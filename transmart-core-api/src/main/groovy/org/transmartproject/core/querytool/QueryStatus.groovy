package org.transmartproject.core.querytool

/**
 * An enumeration for query statuses. This is a static version of a subset of
 * i2b2's qt_query_status_type table.
 */
enum QueryStatus {
    PROCESSING  (2),
    FINISHED    (3),
    ERROR       (4),
    INCOMPLETE  (5),
    COMPLETED   (6)

    final int id

    protected QueryStatus(id) {
        this.id = id
    }

    static QueryStatus forId(int id) {
        values().find { it.id == id }
    }
}
