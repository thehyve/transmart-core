package org.transmartproject.db.i2b2data

class TrialVisit {

    long id
    String relTimeUnit
    Integer relTime
    String relTimeLabel
    Study study


    static constraints = {
        relTimeUnit     nullable: true
        relTime         nullable: true
        relTimeLabel    nullable: true
    }

    static hasMany = [
        observationFacts: ObservationFact
    ]

    static mapping = {
        table schema: 'I2B2DEMODATA'
        id            generator: 'assigned', column: 'id', type: Long

    }
}
