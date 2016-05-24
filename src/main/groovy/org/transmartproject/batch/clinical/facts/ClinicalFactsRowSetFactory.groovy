package org.transmartproject.batch.clinical.facts

import com.google.common.collect.Maps
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.clinical.xtrial.XtrialMappingCollection
import org.transmartproject.batch.clinical.xtrial.XtrialNode
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.concept.ConceptType
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.patient.PatientSet

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

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    public static final int MAX_STRING_LENGTH = 255
    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    ConceptTree tree

    @Autowired
    PatientSet patientSet

    @Autowired
    XtrialMappingCollection xtrialMapping

    @Value("#{clinicalJobContext.variables}")
    List<ClinicalVariable> variables

    @Lazy
    Map<String, ClinicalDataFileVariables> fileVariablesMap = generateVariablesMap()

    private final Map<ConceptNode, XtrialNode> conceptXtrialMap = Maps.newHashMap()

    ClinicalFactsRowSet create(ClinicalDataRow row) {
        ClinicalDataFileVariables fileVariables = fileVariablesMap[row.filename]

        ClinicalFactsRowSet result = new ClinicalFactsRowSet()
        result.studyId = studyId
        result.siteId = fileVariables.getSiteId(row)
        result.visitName = fileVariables.getVisitName(row)

        def patient = patientSet[fileVariables.getPatientId(row)]
        patient.putDemographicValues(fileVariables.getDemographicVariablesValues(row))
        result.patient = patient

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

            def conceptNode = getOrGenerateConceptNode fileVariables, var, row
            def xtrialNode = getXtrialNodeFor(conceptNode)
            result.addValue conceptNode, xtrialNode, value
        }

        result
    }

    private ConceptNode getOrGenerateConceptNode(ClinicalDataFileVariables variables,
                                                 ClinicalVariable var,
                                                 ClinicalDataRow row) {
        /*
         * Concepts are created and assigned types and ids
         */

        ConceptType conceptType
        ConceptPath conceptPath = getOrGenerateConceptPath(variables, var, row)
        ConceptNode concept = tree[conceptPath]
        String value = row[var.columnNumber]

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
            concept = tree.getOrGenerate(conceptPath, conceptType)
        } else { // the concept does already exist (ie not first record)
            conceptType = concept.type

            boolean curValIsNumerical = value.isDouble()

            if (conceptType == ConceptType.NUMERICAL && !curValIsNumerical) {
                throw new IllegalArgumentException("Variable $var inferred or specified " +
                        "numerical, but got value '$value'. Patient id: " +
                        "${variables.getPatientId(row)}.")
            }

        }

        // we need a subnode if the variable is categorical
        if (conceptType == ConceptType.CATEGORICAL) {
            concept = tree.getOrGenerate(conceptPath + value, ConceptType.CATEGORICAL)
        }

        concept
    }

    private ConceptPath getOrGenerateConceptPath(ClinicalDataFileVariables variables,
                                       ClinicalVariable var,
                                       ClinicalDataRow row) {
        if (var.conceptPath) {
            return var.conceptPath
        }

        assert var.dataLabel == ClinicalVariable.TEMPLATE

        def relConceptPath = ConceptFragment.decode(var.categoryCode).path

        if (relConceptPath.contains(ClinicalVariable.SITE_ID_PLACEHOLDER)) {
            def replaceValue = variables.getSiteId(row) ?: ''
            relConceptPath = relConceptPath.replace(ClinicalVariable.SITE_ID_PLACEHOLDER, replaceValue)
        }

        if (relConceptPath.contains(ClinicalVariable.VISIT_NAME_PLACEHOLDER)) {
            def replaceValue = variables.getVisitName(row) ?: ''
            relConceptPath = relConceptPath.replace(ClinicalVariable.VISIT_NAME_PLACEHOLDER, replaceValue)
        }

        ClinicalVariable referencedDataLabelVariable = null
        if (var.dataLabelSource) {
            referencedDataLabelVariable = variables.dataLabelsColumnNumberIndex.get(var.dataLabelSource)
        } else {
            referencedDataLabelVariable = variables.dataLabelsColumnNumberIndex.values().first()
        }
        if (referencedDataLabelVariable) {
            String dataLabelValue = row[referencedDataLabelVariable.columnNumber] ?: ''
            if (relConceptPath.contains(ClinicalVariable.DATA_LABEL_PLACEHOLDER)) {
                relConceptPath = relConceptPath.replace(ClinicalVariable.DATA_LABEL_PLACEHOLDER, dataLabelValue)
            } else if (dataLabelValue) {
                return topNodePath + relConceptPath + dataLabelValue
            }
        }

        topNodePath + relConceptPath
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

    private Map<String, ClinicalDataFileVariables> generateVariablesMap() {
        Map<String, List<ClinicalVariable>> map = variables.groupBy { it.filename }
        map.collectEntries {
            [(it.key): ClinicalDataFileVariables.fromVariableList(it.value)]
        }
    }

}
