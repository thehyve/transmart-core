package org.transmartproject.batch.clinical.facts

import com.google.common.collect.Maps
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.patient.Patient
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.clinical.xtrial.XtrialMappingCollection
import org.transmartproject.batch.clinical.xtrial.XtrialNode
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.concept.ConceptType


/**
 * Creates {@link org.transmartproject.batch.facts.ClinicalFactsRowSet} objects.
 *
 * Because concepts can only be created and assigned types when the data is seen,
 * that is also done here.
 */
@Component
@JobScope
@Slf4j
@TypeChecked
class ClinicalFactsRowSetFactory {

    public static final int MAX_STRING_LENGTH = 255
    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    ConceptTree tree

    @Autowired
    XtrialMappingCollection xtrialMapping

    private final Map<ConceptNode, XtrialNode> conceptXtrialMap = Maps.newHashMap()

    ClinicalFactsRowSet create(ClinicalDataFileVariables fileVariables,
                               ClinicalDataRow row,
                               Patient patient) {
        ClinicalFactsRowSet result = new ClinicalFactsRowSet()
        result.studyId = studyId

        result.patient = patient
        result.siteId = fileVariables.getSiteId(row)
        result.visitName = fileVariables.getVisitName(row)

        fileVariables.otherVariables.each { ClinicalVariable var ->
            String value = row[var.columnNumber]

            if (!value) {
                return
            }

            if (value.length() > MAX_STRING_LENGTH) {
                value = value.take(MAX_STRING_LENGTH)
                log.warn "Found value longer than allowed 255 chars: '${value}'. " +
                        'It will be truncated. ' +
                        "Variable: $var, line ${row.index} of ${row.filename}}"
            }

            processVariableValue result, var, value
        }

        result
    }


    private void processVariableValue(ClinicalFactsRowSet result,
                                      ClinicalVariable var,
                                      String value) {
        /*
         * Concepts are created and assigned types and ids
         */

        ConceptType conceptType
        ConceptNode concept = tree[var.conceptPath]

        // if the concept doesn't yet exist (ie first record)
        if (!concept) {
            conceptType = getConceptTypeFromColumnsFile(var)
            if (conceptType == ConceptType.UNKNOWN) {
                // if no conceptType is set in the column mapping file,
                // try to detect the conceptType from the first record

                conceptType = value.isDouble() ?
                        ConceptType.NUMERICAL : ConceptType.CATEGORICAL
            }

            // has the side-effect of assigning type if it's unknown and
            // creating the concept from scratch if it doesn't exist at all
            concept = tree.getOrGenerate(var.conceptPath, conceptType)
        } else { // the concept does already exist (ie not first record)
            conceptType = concept.type

            boolean curValIsNumerical = value.isDouble()

            if (conceptType == ConceptType.NUMERICAL && !curValIsNumerical) {
                throw new IllegalArgumentException("Variable $var inferred or specified " +
                        "numerical, but got value '$value'. Patient id: " +
                        "${result.patient.id}.")
            }

        }

        // we need a subnode if the variable is categorical
        if (conceptType == ConceptType.CATEGORICAL) {
            concept = tree.getOrGenerate(var.conceptPath + value, ConceptType.CATEGORICAL)
        }

        tree.reserveIdsFor concept

        result.addValue concept, getXtrialNodeFor(concept), value
    }

    private ConceptType getConceptTypeFromColumnsFile(ClinicalVariable var) {
        ConceptType conceptType

        switch (var.conceptType) {
            case ClinicalVariable.CONCEPT_TYPE_CATEGORICAL:
                conceptType = ConceptType.CATEGORICAL
                break

            case ClinicalVariable.CONCEPT_TYPE_NUMERICAL:
                conceptType = ConceptType.NUMERICAL
                break

            case null:
            case '':
                conceptType = ConceptType.UNKNOWN
                break

            default:
                throw new IllegalStateException(
                        "Invalid value for concept type column: $var.conceptType. This " +
                                'should never happen (ought to have been validated)')
        }

        conceptType
    }



    XtrialNode getXtrialNodeFor(ConceptNode conceptNode) {
        if (conceptXtrialMap.containsKey(conceptNode)) {
            conceptXtrialMap[conceptNode]
        } else {
            conceptXtrialMap[conceptNode] = xtrialMapping.findMappedXtrialNode(
                    conceptNode.path,
                    conceptNode.type)
        }
    }

}
