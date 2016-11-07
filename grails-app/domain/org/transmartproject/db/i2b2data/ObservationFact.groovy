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

    /* links to concept_dimension, but concept_code is not primary key in
     * concept_dimension. Hence, i2b2 1.3 added a concept_path column here */
    String     conceptCode

    String     valueType
    String     textValue
    BigDecimal numberValue
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
    // unsed for now
    //BigDecimal quantityNum
    //String     unitsCd
    //String     observationBlob
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
        valueFlag         nullable:   true,   maxSize:   50
        sourcesystemCd    nullable:   true,   maxSize:   50
        endDate           nullable:   true
        locationCd        nullable:   true,   maxSize:   50

        // unused for now
        //quantityNum       nullable:   true,   scale:     5
        //unitsCd           nullable:   true,   maxSize:   50
        //observationBlob   nullable:   true
        //confidenceNum     nullable:   true,   scale:     5
        //updateDate        nullable:   true
        //downloadDate      nullable:   true
        //importDate        nullable:   true
        //uploadId          nullable:   true
    }

    VisitDimension getVisit() {
        VisitDimension.get(new VisitDimension(patient: patient, encounterNum: encounterNum))
    }

    def getValue() {
        observationFactValue(valueType, textValue, numberValue)
    }

    // Separate static method so this can also be called from code that accesses the database without converting to
    // domain classes
    @CompileStatic
    static final def observationFactValue(String valueType, String textValue, BigDecimal numberValue) {
        switch(valueType) {
            case TYPE_TEXT:
                return textValue
            case TYPE_NUMBER:
                return numberValue
            default:
                throw new DataInconsistencyException("Inconsistent database value: ObservationFact.valueType " +
                        "must be either '$TYPE_TEXT' or '$TYPE_NUMBER'. Found '${valueType}'")
        }
    }
}
