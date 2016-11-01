package org.transmartproject.db.i2b2data

import org.apache.commons.lang.NotImplementedException
import org.transmartproject.db.metadata.DimensionDescription

class Study {

    long id

    String name

    static constraints = {
    }

    static hasMany = [
        trialVisits: TrialVisit,
        dimensions: DimensionDescription,
    ]

    static mapping = {
        table schema: 'I2B2DEMODATA'
        id    name:   'id'

    }
}
