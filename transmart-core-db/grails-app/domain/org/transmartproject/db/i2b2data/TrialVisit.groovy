package org.transmartproject.db.i2b2data

class TrialVisit {

    long id
    String relTimeUnit
    Integer relTime
    String relTimeLabel



    static constraints = {
        relTimeUnit     nullable: true
        relTime         nullable: true
        relTimeLabel    nullable: true
    }

    static belongsTo = {
        study: Study
    }
    static mapping = {
        table schema: 'I2B2DEMODATA'
        id            generator: 'assigned', column: 'id', type: Long

    }
}
