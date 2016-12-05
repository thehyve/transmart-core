package org.transmartproject.core.dataquery

public enum Sex {

    MALE,
    FEMALE,
    UNKNOWN

    String toString() {
        this.name().toLowerCase(Locale.ENGLISH)
    }

    static Sex fromString(String name) {
        Sex.values().find {
            it.toString() == name?.toLowerCase(Locale.ENGLISH)
        } ?: UNKNOWN
    }
}
