package org.transmartproject.batch.highdim.mrna.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * Validates {@link MrnaAnnotationRow} objects.
 */
@Component
@JobScope
@Slf4j
class MrnaAnnotationRowValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        clazz == MrnaAnnotationRow
    }

    @Override
    @SuppressWarnings('ReturnNullFromCatchBlock')
    void validate(Object target, Errors errors) {
        assert target instanceof MrnaAnnotationRow

        if (!target.probeName) {
            errors.rejectValue 'probeName', 'required',
                    ['probeName'] as Object[], null
        }

        try {
            // we rely on entrezIdList to force string -> long conversion
            target.entrezIdList // called for collaterals
        } catch (NumberFormatException nfe) {
            errors.rejectValue 'entrezIds', 'expectedSeparatedLongs',
                    [target.entrezIds] as Object[], null
            return // the rest depends on a valid entrezIdList
        }

        if (target.geneList.size() !=
                target.entrezIdList.size()) {
            errors.rejectValue null, 'sizeMismatch',
                    ['genes', target.geneList.size(),
                     'entrezIds', target.entrezIdList.size(),
                     target.geneList, target.entrezIdList] as Object[],
                    null
        } else {
            def combined = [target.geneList, target.entrezIdList].transpose()

            if (combined.any {
                it[0] == null && it[1] != null ||
                        it[1] == null && it[0] != null
            }) {
                log.warn("Found gene without entrez id or vice-versa. " +
                        "Gene list: ${target.geneList}, " +
                        "entrez id list: ${target.entrezIdList}. I'll allow it.")
            }
        }
    }
}
