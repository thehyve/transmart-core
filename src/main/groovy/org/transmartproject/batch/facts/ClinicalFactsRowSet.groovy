package org.transmartproject.batch.facts

import org.transmartproject.batch.clinical.xtrial.XtrialNode
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptType
import org.transmartproject.batch.patient.Patient

/**
 * Contains the transformed meaningful information from one data Row</br>
 * This includes the Patient and values for all Variables in a row of a file
 */
class ClinicalFactsRowSet {
    String studyId
    Patient patient
    String siteId
    String visitName

    private final static Date BOGUS_START_DATE = new GregorianCalendar(1970, 0, 1).time

    private final Date date = new Date()

    final List<ClinicalFact> clinicalFacts = []

    void addValue(ConceptNode concept, XtrialNode xtrialNode, String value) {
        clinicalFacts << new ClinicalFact(
                concept: concept,
                xtrialNode: xtrialNode,
                value: value,)
    }

    class ClinicalFact {
        String value
        ConceptNode concept
        XtrialNode xtrialNode

        String getValueTypeCode() {
            switch (concept.type) {
                case ConceptType.NUMERICAL:
                    return 'N'
                case ConceptType.HIGH_DIMENSIONAL:
                case ConceptType.CATEGORICAL:
                    return 'T'
                default:
                    throw new IllegalStateException("Unexpected concept " +
                            "type: $concept.type (concept: $concept)")
            }
        }

        String getStringValue() {
            switch (concept.type) {
                case ConceptType.NUMERICAL:
                    return 'E' // value is Equal to the numeric value column
                case ConceptType.HIGH_DIMENSIONAL:
                case ConceptType.CATEGORICAL:
                    return value
                default:
                    throw new IllegalStateException("Unexpected concept " +
                            "type: $concept.type (concept: $concept)")
            }
        }

        Double getNumericValue() {
            switch (concept.type) {
                case ConceptType.NUMERICAL:
                    return Double.valueOf(value)
                case ConceptType.HIGH_DIMENSIONAL:
                case ConceptType.CATEGORICAL:
                    return null
                default:
                    throw new IllegalStateException(
                            "Unexpected concept type: ${concept.type}")
            }
        }

        /* Probably should be moved to another place (repository or insertion tasklet) */

        Map<String, Object> getDatabaseRow() {
            if (!concept.code) {
                throw new IllegalStateException(
                        "Concept should have code attributed " +
                                "by now, but I found $concept")
            }

            [
                    sourcesystem_cd: studyId,
                    encounter_num  : patient.code,
                    patient_num    : patient.code,
                    concept_cd     : concept.code,
                    //start_date: new Date(Long.MAX_VALUE), //doesn't work
                    valtype_cd     : valueTypeCode,
                    tval_char      : stringValue,
                    nval_num       : numericValue,
                    start_date     : BOGUS_START_DATE, // in i2b2 schema, part of PK
                    import_date    : date,

                    provider_id    : '@',
                    location_cd    : '@',
                    modifier_cd    : xtrialNode?.code ?: '@',
                    valueflag_cd   : '@',
                    instance_num   : 1,
            ]
        }
    }
}
