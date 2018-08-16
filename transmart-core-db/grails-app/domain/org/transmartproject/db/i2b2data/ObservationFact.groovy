/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.i2b2data

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.exceptions.DataInconsistencyException

@EqualsAndHashCode(includes = ['encounterNum', 'patient', 'conceptCode', 'providerId', 'startDate', 'modifierCd',
        'instanceNum'])
class ObservationFact implements Serializable {

    public static String TYPE_TEXT   = 'T'
    public static String TYPE_NUMBER = 'N'
    public static String TYPE_DATE = 'D'
    public static String TYPE_RAW_TEXT = 'B'
    public static String EMPTY_MODIFIER_CODE = '@'
    public static Set<String> ALL_TYPES = [TYPE_TEXT, TYPE_NUMBER, TYPE_RAW_TEXT, TYPE_DATE]
    //TODO Remove in TMT-420
    public static Set<String> NUMBER_FIELD_TYPES = [TYPE_NUMBER, TYPE_DATE]

    /* links to concept_dimension, but concept_code is not primary key in
     * concept_dimension. Hence, i2b2 1.3 added a concept_path column here */
    String     conceptCode

    String     valueType
    String     textValue
    BigDecimal numberValue
    String     rawValue
    String     valueFlag
    String     sourcesystemCd

    BigDecimal encounterNum
    String     providerId
    Date       startDate
    Date       endDate
    String     modifierCd
    Long       instanceNum
    String     locationCd

    //TrialVisit trialVisit
    //VisitDimension visit
    // unused for now
    //BigDecimal quantityNum
    //String     unitsCd
    //BigDecimal confidenceNum
    //Date       updateDate
    //Date       downloadDate
    //Date       importDate
    //BigDecimal uploadId

    static belongsTo = [
        patient      : PatientDimension,
        trialVisit   : TrialVisit,
    ]

    static mapping = {
        table        name: 'observation_fact', schema: 'I2B2DEMODATA'

        id           composite: ['encounterNum', 'patient', 'conceptCode', 'providerId', 'startDate', 'modifierCd',
                                 'instanceNum']

        conceptCode  column: 'concept_cd'
        patient      column: 'patient_num'
        startDate    column: 'start_date'
        modifierCd   column: 'modifier_cd'
        valueType    column: 'valtype_cd'
        textValue    column: 'tval_char'
        numberValue  column: 'nval_num'
        rawValue     column: 'observation_blob', sqlType: 'text'
        valueFlag    column: 'valueflag_cd'
        trialVisit   column: 'trial_visit_num', cascade: 'save-update'

        version false
    }

    static constraints = {
        conceptCode       maxSize:    50
        providerId        maxSize:    50
        modifierCd        maxSize:    100
        valueType         nullable:   true,   maxSize:   50
        textValue         nullable:   true
        numberValue       nullable:   true,   scale:     5
        rawValue          nullable:   true
        valueFlag         nullable:   true,   maxSize:   50
        sourcesystemCd    nullable:   true,   maxSize:   50
        endDate           nullable:   true
        locationCd        nullable:   true,   maxSize:   50

        // unused for now
        //quantityNum       nullable:   true,   scale:     5
        //unitsCd           nullable:   true,   maxSize:   50
        //confidenceNum     nullable:   true,   scale:     5
        //updateDate        nullable:   true
        //downloadDate      nullable:   true
        //importDate        nullable:   true
        //uploadId          nullable:   true
    }

    VisitDimension getVisit() {
        VisitDimension.get(new VisitDimension(patient: patient, encounterNum: encounterNum))
    }

    @CompileStatic
    def getValue() {
        observationFactValue(valueType, textValue, numberValue, rawValue)
    }

    // Separate static method so this can also be called from code that accesses the database without converting to
    // domain classes
    @CompileStatic
    static final Object observationFactValue(String valueType, String textValue, BigDecimal numberValue, String rawValue) {
        switch(valueType) {
            case TYPE_TEXT:
                return textValue
            case TYPE_NUMBER:
                return numberValue
            case TYPE_DATE:
                return new Date(numberValue.longValue())
            case TYPE_RAW_TEXT:
                return rawValue
            default:
                throw new DataInconsistencyException("Unsupported database value: ObservationFact.valueType " +
                        "must be one of ${ALL_TYPES.join(', ')}. Found '${valueType}'.")
        }
    }
}
