package org.transmartproject.core.dataquery

public enum Sex {

    MALE,
    FEMALE,
    UNKNOWN

    String toString() {
        this.name().toLowerCase(Locale.ENGLISH)
    }

    // Warning: this is not a valid method to convert a PatientDimension.sexCd to a Sex enum! Use PatientDimension
    // .getSex() for that!
    static Sex fromString(String name) {
        Sex.values().find {
            it.toString() == name?.toLowerCase(Locale.ENGLISH)
        } ?: UNKNOWN
    }
}
