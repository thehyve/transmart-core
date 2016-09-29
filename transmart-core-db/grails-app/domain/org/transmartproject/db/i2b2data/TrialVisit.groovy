package org.transmartproject.db.i2b2data

class TrialVisit {

    long id
    String relTimeUnit
    int relTime
    String relTimeLabel



    static constraints = {
        relTimeUnit     nullable: true
        relTime         nullable: true
        relTimeLabel    nullable: true
    }
}
