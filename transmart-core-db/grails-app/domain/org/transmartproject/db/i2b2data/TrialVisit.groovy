/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.i2b2data

class TrialVisit {

    String relTimeUnit
    Integer relTime
    String relTimeLabel

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
        table           name: 'trial_visit_dimension', schema: 'I2B2DEMODATA'
        id              column: 'trial_visit_num', type: Long, generator: 'sequence', params: [sequence: 'trial_visit_num_seq']
        study           column: 'study_num', cascade: 'save-update'
        relTimeUnit     column: 'rel_time_unit_cd'
        relTime         column: 'rel_time_num'
        relTimeLabel    column: 'rel_time_label'
        version false
    }
}
