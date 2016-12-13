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
import org.transmartproject.batch.concept.*
import org.transmartproject.batch.facts.ClinicalFactsRowSet
import org.transmartproject.batch.patient.PatientSet
import org.transmartproject.batch.secureobject.Study

import java.text.SimpleDateFormat

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
    Study study

    @Autowired
    ConceptTree tree

    @Autowired
    PatientSet patientSet

    @Autowired
    XtrialMappingCollection xtrialMapping

    @Value("#{clinicalJobContext.variables}")
    List<ClinicalVariable> variables

    SimpleDateFormat dateFormatter = new SimpleDateFormat('yyyy-MM-dd')

    @Lazy
    Map<String, ClinicalDataFileVariables> fileVariablesMap = generateVariablesMap()

    private final Map<ConceptNode, XtrialNode> conceptXtrialMap = Maps.newHashMap()

    ClinicalFactsRowSet create(ClinicalDataRow row) {
        ClinicalDataFileVariables fileVariables = fileVariablesMap[row.filename]

        ClinicalFactsRowSet result = new ClinicalFactsRowSet()
        result.studyId = studyId
        result.siteId = fileVariables.getSiteId(row)
        result.visitName = fileVariables.getVisitName(row)
        def startDate = fileVariables.getStartDate(row)
        if (startDate) {
            result.startDate = dateFormatter.parse(startDate)
        }
        def endDate = fileVariables.getEndDate(row)
        if (endDate) {
            result.endDate = dateFormatter.parse(endDate)
        }
        result.trialVisit = study.getTrialVisit(fileVariables.getTrialVisitLabel(row))
        String num = fileVariables.getInstanceNum(row)
        if (num) {
            result.instanceNum = Integer.parseInt(num)
        }

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

    /**
     * Sets the concept type if it is not set ({@link ConceptType#UNKNOWN} and
     * checks if the value is numerical if the concept is.
     *
     * @throw IllegalArgumentException if the concept type is numerical and the value
     * is not numerical.
     */
    private static void updateConceptType(ConceptNode concept, ClinicalVariable var, String value) {
        if (concept.type == ConceptType.UNKNOWN) {
            def conceptType = ClinicalVariable.conceptTypeFor(var)
            if (conceptType == ConceptType.UNKNOWN) {
                // if no conceptType is set in the column mapping file,
                // try to detect the conceptType from the value
                conceptType = value.isDouble() ?
                        ConceptType.NUMERICAL : ConceptType.CATEGORICAL
            }
            concept.type = conceptType
            log.debug("Assigning type ${concept.type} to concept node ${concept.path}")
        }
        if (concept.type == ConceptType.NUMERICAL && !value.isDouble()) {
            throw new IllegalArgumentException("Variable $var inferred or specified " +
                    "numerical, but got value '$value'.")
        }
    }

    private ConceptNode getOrGenerateConceptNode(ClinicalDataFileVariables variables,
                                                 ClinicalVariable var,
                                                 ClinicalDataRow row) {
        /*
         * Concepts are created and assigned types and ids
         */
        ConceptPath conceptPath = getOrGenerateConceptPath(variables, var, row)
        ConceptNode concept = tree.conceptNodeForConceptPath(conceptPath)
        String value = row[var.columnNumber]

        // if the concept doesn't yet exist (ie first record)
        if (!concept) {
            concept = tree.getOrGenerateConceptForVariable(var)
        }
        updateConceptType(concept, var, value)

        // we need a subnode if the variable is categorical
        if (concept.type == ConceptType.CATEGORICAL && !'y'.equalsIgnoreCase(var.strictCategoricalVariable)) {
            concept = tree.getOrGenerate(new ConceptPath(conceptPath) + value, var, ConceptType.CATEGORICAL)
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
