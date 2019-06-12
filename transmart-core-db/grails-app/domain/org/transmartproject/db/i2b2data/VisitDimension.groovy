/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.i2b2data

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic

class VisitDimension {

    PatientDimension    patient
    String              activeStatusCd
    Date                startDate
    Date                endDate
    String              inoutCd
    String              locationCd
    String              locationPath
    BigDecimal          lengthOfStay
    String              visitBlob
    Date                updateDate
    Date                downloadDate
    Date                importDate
    String              sourcesystemCd
    BigDecimal          uploadId

    static hasMany = [mappings: EncounterMapping]

    static mappedBy = [mappings: 'visit']

    static mapping = {
        table           name: 'visit_dimension', schema: 'I2B2DEMODATA'
        id              column: 'encounter_num', type: Long
        patient         column: 'patient_num'
        activeStatusCd  column: 'active_status_cd'
        startDate       column: 'start_date'
        endDate         column: 'end_date'
        inoutCd         column: 'inout_cd'
        locationCd      column: 'location_cd'
        locationPath    column: 'location_path'
        lengthOfStay    column: 'length_of_stay'
        visitBlob       column: 'visit_blob'
        updateDate      column: 'update_date'
        downloadDate    column: 'download_date'
        importDate      column: 'import_date'
        sourcesystemCd  column: 'sourcesystem_cd'
        uploadId        column: 'upload_id'
        mappings        fetch:  'join'
        version         false
    }

    static constraints = {
        activeStatusCd    nullable:   true,   maxSize:   50
        startDate         nullable:   true
        endDate           nullable:   true
        inoutCd           nullable:   true,   maxSize:   50
        locationCd        nullable:   true,   maxSize:   50
        locationPath      nullable:   true,   maxSize:   900
        lengthOfStay      nullable:   true
        visitBlob         nullable:   true
        updateDate        nullable:   true
        downloadDate      nullable:   true
        importDate        nullable:   true
        sourcesystemCd    nullable:   true,   maxSize:   50
        uploadId          nullable:   true
    }

    String getPatientInTrialId() {
        patient.inTrialId
    }

    @Lazy
    ImmutableMap<String, String> encounterIds = computeEncounterIds()

    @CompileStatic
    private ImmutableMap<String,String> computeEncounterIds () {
        ImmutableMap.Builder<String,String> builder = ImmutableMap.builder()
        if (mappings != null) for(def mapping : mappings) {
            builder.put(mapping.source, mapping.encryptedId)
        }
        builder.build()
    }

}
