package org.transmartproject.db.i2b2data

class TrialVisit {

    String relTimeUnit
    Integer relTime
    String relTimeLabel
    //Study study


    static constraints = {
        relTimeUnit     nullable: true
        relTime         nullable: true
        relTimeLabel    nullable: true
    }

    static hasMany = [
        observationFacts: ObservationFact
    ]

    static belongsTo = [
            study: Study
    ]

    static mapping = {
        table schema: 'I2B2DEMODATA'
        id    name:   'id', type: Long
        study cascade: 'save-update'
    }
}
