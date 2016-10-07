package org.transmartproject.db.i2b2data

class Study {

    long id

    String name

    static constraints = {
    }

    static hasMany = [
        trialVisits: TrialVisit
    ]

    static mapping = {
        table schema: 'I2B2DEMODATA'
        id    name:   'id', generator: 'assigned'

    }
}
