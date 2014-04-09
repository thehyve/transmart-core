package org.transmartproject.db.i2b2data

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = ['encounterNum', 'conceptCode', 'providerId', 'startDate', 'modifierCd', 'instanceNum'])
class ObservationFact implements Serializable {

    public static String TYPE_TEXT   = 'T'
    public static String TYPE_NUMBER = 'N'

    /* links to concept_dimension, but concept_code is not primary key in
     * concept_dimension. Hence, i2b2 1.3 added a concept_path column here */
    String     conceptCode

    String     valueType
    String     textValue
    BigDecimal numberValue
    String     valueFlag
    String     sourcesystemCd


    // these are not used, but we need them because they're not nullable
    BigDecimal encounterNum
    String     providerId
    Date       startDate
    String     modifierCd
    Long       instanceNum

    ConceptVisit conceptVisit

    // unsed for now
    //BigDecimal quantityNum
    //String     unitsCd
    //Date       endDate
    //String     locationCd
    //String     observationBlob
    //BigDecimal confidenceNum
    //Date       updateDate
    //Date       downloadDate
    //Date       importDate
    //BigDecimal uploadId

    static belongsTo = [
            patient: PatientDimension,
    ]

    static mapping = {
        table        name: 'observation_fact', schema: 'I2B2DEMODATA'

        id           composite: ['encounterNum', 'patient', 'conceptCode', 'providerId', 'startDate', 'modifierCd']

        conceptCode  column: 'concept_cd'
        patient      column: 'patient_num'
        valueType    column: 'valtype_cd'
        textValue    column: 'tval_char'
        numberValue  column: 'nval_num'
        valueFlag    column: 'valueflag_cd'

        conceptVisit	column: 'CONCEPT_CD', type: 'string', insertable: false, updateable: false

        version false
    }

    static constraints = {
        patient           nullable:   true
        conceptCode       maxSize:    50
        providerId        maxSize:    50
        modifierCd        maxSize:    100
        valueType         nullable:   true,   maxSize:   50
        textValue         nullable:   true
        numberValue       nullable:   true,   scale:     5
        valueFlag         nullable:   true,   maxSize:   50
        sourcesystemCd    nullable:   true,   maxSize:   50


        // unused for now
        //quantityNum       nullable:   true,   scale:     5
        //unitsCd           nullable:   true,   maxSize:   50
        //endDate           nullable:   true
        //locationCd        nullable:   true,   maxSize:   50
        //observationBlob   nullable:   true
        //confidenceNum     nullable:   true,   scale:     5
        //updateDate        nullable:   true
        //downloadDate      nullable:   true
        //importDate        nullable:   true
        //uploadId          nullable:   true
    }
}
