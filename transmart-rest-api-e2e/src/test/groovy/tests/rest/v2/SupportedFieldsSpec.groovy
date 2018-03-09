/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.EHR_ID
import static config.Config.PATH_SUPPORTED_FIELDS

class SupportedFieldsSpec extends RESTSpec {

    /**
     *  given: "any studies are loaded"
     *  when: "I request all supported fields"
     *  then: "I get a list of fields with dimension, fieldName and type"
     */
    @RequiresStudy(EHR_ID)
    def "supported fields"() {
        given: "any studies are loaded"

        when: "I request all supported fields"
        def responseData = get([
                path      : PATH_SUPPORTED_FIELDS,
                acceptType: JSON
        ])

        then: "I get a list of fields with dimension, fieldName and type"
        responseData.each {
            assert supportedFields.contains(it)
        }
    }

    def supportedFields = [[dimension: 'patient', fieldName: 'id', type: 'ID'], [dimension: 'patient', fieldName: 'age', type: 'NUMERIC'],
                           [dimension: 'patient', fieldName: 'mappings', type: 'OBJECT'],
                           [dimension: 'patient', fieldName: 'race', type: 'STRING'], [dimension: 'patient', fieldName: 'maritalStatus', type: 'STRING'],
                           [dimension: 'patient', fieldName: 'religion', type: 'STRING'], [dimension: 'patient', fieldName: 'patientBlob', type: 'STRING'],
                           [dimension: 'patient', fieldName: 'vitalStatusCd', type: 'STRING'], [dimension: 'patient', fieldName: 'birthDate', type: 'DATE'],
                           [dimension: 'patient', fieldName: 'deathDate', type: 'DATE'], [dimension: 'patient', fieldName: 'sexCd', type: 'STRING'], [dimension: 'patient', fieldName: 'languageCd', type: 'STRING'],
                           [dimension: 'patient', fieldName: 'zipCd', type: 'STRING'], [dimension: 'patient', fieldName: 'statecityzipPath', type: 'STRING'], [dimension: 'patient', fieldName: 'incomeCd', type: 'STRING'],
                           [dimension: 'patient', fieldName: 'updateDate', type: 'DATE'], [dimension: 'patient', fieldName: 'downloadDate', type: 'DATE'], [dimension: 'patient', fieldName: 'importDate', type: 'DATE'],
                           [dimension: 'patient', fieldName: 'sourcesystemCd', type: 'STRING'], [dimension: 'patient', fieldName: 'uploadId', type: 'NUMERIC'], [dimension: 'concept', fieldName: 'conceptCode', type: 'STRING'],
                           [dimension: 'visit', fieldName: 'encounterNum', type: 'NUMERIC'], [dimension: 'visit', fieldName: 'patient', type: 'OBJECT'], [dimension: 'visit', fieldName: 'activeStatusCd', type: 'STRING'],
                           [dimension: 'visit', fieldName: 'startDate', type: 'DATE'], [dimension: 'visit', fieldName: 'endDate', type: 'DATE'], [dimension: 'visit', fieldName: 'inoutCd', type: 'STRING'],
                           [dimension: 'visit', fieldName: 'locationCd', type: 'STRING'], [dimension: 'visit', fieldName: 'locationPath', type: 'STRING'], [dimension: 'visit', fieldName: 'lengthOfStay', type: 'NUMERIC'],
                           [dimension: 'visit', fieldName: 'visitBlob', type: 'STRING'], [dimension: 'visit', fieldName: 'updateDate', type: 'DATE'], [dimension: 'visit', fieldName: 'downloadDate', type: 'DATE'],
                           [dimension: 'visit', fieldName: 'importDate', type: 'DATE'], [dimension: 'visit', fieldName: 'sourcesystemCd', type: 'STRING'], [dimension: 'visit', fieldName: 'uploadId', type: 'NUMERIC'],
                           [dimension: 'trial visit', fieldName: 'id', type: 'ID'], [dimension: 'trial visit', fieldName: 'study', type: 'OBJECT'], [dimension: 'trial visit', fieldName: 'relTimeUnit', type: 'STRING'],
                           [dimension: 'trial visit', fieldName: 'relTime', type: 'NUMERIC'], [dimension: 'trial visit', fieldName: 'relTimeLabel', type: 'STRING'],
                           [dimension: 'location', fieldName: 'locationCd', type: 'STRING'], [dimension: 'provider', fieldName: 'providerId', type: 'STRING'],
                           [dimension: 'start time', fieldName: 'startDate', type: 'DATE'], [dimension: 'end time', fieldName: 'endDate', type: 'DATE']]
}
