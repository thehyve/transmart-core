package org.transmartproject.batch.clinical.variable

import com.google.common.base.Function
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.support.MappingHelper

/**
 * Converts TSV file lines into {@link ClinicalVariable}s.
 * TODO: replace with chunk step that correctly reads tsv file
 */
class VariableLineMapper implements Function<String,ClinicalVariable> {

    ConceptPath topNodePath

    @Override
    ClinicalVariable apply(String input) {
        ClinicalVariable result = MappingHelper.parseObject(input, ClinicalVariable, ClinicalVariable.FIELDS)
        result.columnNumber-- //index is now 0 based
        if (!ClinicalVariable.RESERVED.contains(result.dataLabel)) {
            ConceptPath path = topNodePath +
                    toPath(result.categoryCode) +
                    toPath(result.dataLabel)

            result.conceptPath = path
        }
        result
    }

    private static String toPath(String columnMappingPathFragment) {
        columnMappingPathFragment
                .replace('+', '\\')
                .replace('_', ' ')
    }
}
