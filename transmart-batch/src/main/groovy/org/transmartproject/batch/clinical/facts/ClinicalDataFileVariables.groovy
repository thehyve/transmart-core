package org.transmartproject.batch.clinical.facts

import groovy.util.logging.Slf4j
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.patient.DemographicVariable

/**
 * Holds the variables defined for a file
 */
@Slf4j
class ClinicalDataFileVariables {
    ClinicalVariable subjectIdVariable
    ClinicalVariable siteIdVariable
    ClinicalVariable visitNameVariable
    List<ClinicalVariable> otherVariables = []
    List<ClinicalVariable> demographicRelated = []
    Map<Integer, ClinicalVariable> dataLabelsColumnNumberIndex = [:]

    static ClinicalDataFileVariables fromVariableList(List<ClinicalVariable> list) {
        def otherVariables = []
        def args = [:]
        def demographic = []
        def templates = []
        Map<Integer, ClinicalVariable> dataLabelsColumnNumberIndex = [:]
        list.each {
            switch (it.dataLabel) {
                case ClinicalVariable.SUBJ_ID:
                    args.put('subjectIdVariable', it)
                    break
                case ClinicalVariable.SITE_ID:
                    args.put('siteIdVariable', it)
                    break
                case ClinicalVariable.VISIT_NAME:
                    args.put('visitNameVariable', it)
                    break
                case ClinicalVariable.DATA_LABEL:
                    dataLabelsColumnNumberIndex.put(it.columnNumber, it)
                    break
                case ClinicalVariable.TEMPLATE:
                    templates.add(it)
                    otherVariables.add(it)
                    break
                case ClinicalVariable.OMIT:
                case ClinicalVariable.STUDY_ID:
                    //ignore
                    break
                default:
                    otherVariables.add(it)
            }
            if (it.demographic) {
                demographic.add(it)
            }
        }
        args.put('otherVariables', otherVariables)
        args.put('demographicRelated', demographic)

        boolean multipleDataLabelColumns = dataLabelsColumnNumberIndex.size() > 1
        templates.each { ClinicalVariable template ->
            boolean withDataLabelPlaceholder = template.categoryCode.contains(ClinicalVariable.DATA_LABEL_PLACEHOLDER)
            boolean hasDataLabelSource = template.dataLabelSource != null

            if (multipleDataLabelColumns && withDataLabelPlaceholder && !hasDataLabelSource) {
                throw new IllegalArgumentException("Declaration of column #${template.columnNumber + 1} with data" +
                        " label placeholder in category code has to point with data label source to a data label" +
                        " column.")
            }

            if (hasDataLabelSource && !dataLabelsColumnNumberIndex.containsKey(template.dataLabelSource)) {
                throw new IllegalArgumentException("Data label source of column #${template.columnNumber + 1} has to" +
                        " point to existing DATA_LABEL column number.")
            }
        }
        args.put('dataLabelsColumnNumberIndex', dataLabelsColumnNumberIndex)

        new ClinicalDataFileVariables(args)
    }

    String getPatientId(ClinicalDataRow row) {
        row[subjectIdVariable.columnNumber]
    }

    String getSiteId(ClinicalDataRow row) {
        if (siteIdVariable) {
            row[siteIdVariable.columnNumber]
        }
    }

    String getVisitName(ClinicalDataRow row) {
        if (visitNameVariable) {
            row[visitNameVariable.columnNumber]
        }
    }

    Map<DemographicVariable, String> getDemographicVariablesValues(ClinicalDataRow row) {
        demographicRelated.collectEntries { ClinicalVariable var ->
            [var.demographicVariable, row[var.columnNumber]]
        }
    }
}
