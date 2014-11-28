package org.transmartproject.batch.clinical.facts

import groovy.util.logging.Slf4j
import org.transmartproject.batch.clinical.patient.DemographicVariable
import org.transmartproject.batch.clinical.variable.ClinicalVariable

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

    static ClinicalDataFileVariables fromVariableList(List<ClinicalVariable> list) {
        def otherVariables = []
        def args = [:]
        def demographic = []
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
