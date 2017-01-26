package org.transmartproject.batch.facts

import groovy.util.logging.Slf4j
import org.transmartproject.batch.clinical.xtrial.XtrialNode
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptType
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.trialvisit.TrialVisit

/**
 * Contains the transformed meaningful information from one data Row</br>
 * This includes the Patient and values for all Variables in a row of a file
 */
@Slf4j
class ClinicalFactsRowSet {

    final static Date DEFAULT_START_DATE = Date.parse('yyyy-MM-dd HH:mm:ss', '0001-01-01 00:00:00')
    final static Integer DEFAULT_INSTANCE_NUM = 1
    public final static String ORIGINAL_VARIABLE_NAME_MODIFIER = 'TRANSMART:ORIGINAL_VARIABLE'

    String studyId
    Patient patient
    String siteId
    String visitName
    Date startDate
    Date endDate
    Integer instanceNum
    TrialVisit trialVisit

    Date importDate = new Date()

    final List<ClinicalFact> clinicalFacts = []

    void addValue(ConceptNode concept, XtrialNode xtrialNode, String value) {
        clinicalFacts << new ClinicalFact(
                concept: concept,
                xtrialNode: xtrialNode,
                value: value,)
        if (concept.ontologyNode) {
            // Add entry with modifier to indicate the original variable name
            // used for an observation.
            clinicalFacts << new ClinicalFact(
                    concept: concept,
                    modifierCode: ORIGINAL_VARIABLE_NAME_MODIFIER,
                    value: concept.name)
        }
    }

    class ClinicalFact {
        String value
        ConceptNode concept
        String modifierCode
        XtrialNode xtrialNode

        ConceptType getType() {
            modifierCode ? ConceptType.CATEGORICAL : concept.type
        }

        String getValueTypeCode() {
            switch (type) {
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
            switch (type) {
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
            switch (type) {
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
                    encounter_num  : -1,
                    patient_num    : patient.code,
                    concept_cd     : concept.code,
                    //start_date: new Date(Long.MAX_VALUE), //doesn't work
                    valtype_cd     : valueTypeCode,
                    tval_char      : stringValue,
                    nval_num       : numericValue,
                    start_date     : startDate ?: DEFAULT_START_DATE, // in i2b2 schema, part of PK
                    end_date       : endDate,
                    import_date    : importDate,

                    provider_id    : '@',
                    location_cd    : '@',
                    modifier_cd    : xtrialNode?.code ?: modifierCode ?: '@',
                    valueflag_cd   : '@',
                    instance_num   : instanceNum ?: DEFAULT_INSTANCE_NUM,
                    trial_visit_num: trialVisit?.id
            ]
        }
    }
}
