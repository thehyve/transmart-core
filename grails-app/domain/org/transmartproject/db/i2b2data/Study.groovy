package org.transmartproject.db.i2b2data

import org.transmartproject.db.metadata.DimensionDescription

class Study {

    static final String PUBLIC = 'PUBLIC'

    String studyId
    String secureObjectToken

    static constraints = {
    }

    static hasMany = [
        trialVisits: TrialVisit,
        dimensions: DimensionDescription,
    ]

    static mapping = {
        table               name: 'study', schema: 'I2B2DEMODATA'
        id                  column: 'study_num', type: Long
        studyId             column: 'study_id'
        secureObjectToken   column: 'secure_obj_token'
    }

}
