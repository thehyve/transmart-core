package org.transmartproject.batch.highdim.mrna.data.mapping

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.highdim.assays.MappingFileRow
import org.transmartproject.batch.highdim.assays.MappingsFileRowStore

/**
 * Collects the {@link MappingFileRow}s.
 *
 * TODO: This class can probably be generalized for other high dim types.
 *       There's only the need of moving the validator out of here
 */
@Component
@JobScope
@Slf4j
class MrnaMappings implements MappingsFileRowStore {

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Value("#{jobParameters['NODE_NAME']}")
    String nodeName

    private final List<MappingFileRow> rows = []

    @Override
    List<MappingFileRow> getRows() {
        this.@rows
    }

    void leftShift(MappingFileRow row) {
        rows << row
        log.debug "Added $row"
    }

    Validator getValidator() {
        new MrnaMappingRowValidator()
    }

    Set<String> getAllSubjectCodes() {
        rows*.subjectId as Set
    }

    Set<String> getAllSampleCodes() {
        rows*.sampleCd as Set
    }

    MappingFileRow getBySampleName(String sampleName) {
        rows.find {
            it.sampleCd == sampleName
        }
    }

    Set<ConceptPath> getAllConceptPaths() {
        def ret = Sets.newHashSet()
        ConceptPath base = topNode + nodeName
        rows.each {
            ret << base + it.conceptFragment
        }
        ret
    }

    String getPlatform() {
        if (!rows) {
            throw new IllegalStateException("No mapping rows read")
        }
        rows[0].platform
    }

    private class MrnaMappingRowValidator implements Validator {
        boolean supports(Class<?> clazz) {
            clazz == MappingFileRow
        }

        void validate(Object target, Errors errors) {
            assert target instanceof MappingFileRow

            /* mandatory fields */
            if (!target.subjectId) {
                errors.rejectValue 'subjectId', 'required',
                        ['Subject id (code)'] as Object[], null
            }
            if (!target.sampleCd) {
                errors.rejectValue 'sampleCd', 'required',
                        ['Sample code'] as Object[], null
            }
            if (!target.platform) {
                errors.rejectValue 'platform', 'required',
                        ['Platform'] as Object[], null
            }
            if (!target.categoryCd) {
                errors.rejectValue 'categoryCd', 'required',
                        ['Category Code'] as Object[], null
            }

            /* constant field */
            if (rows) {
                if (rows[0].platform != target.platform) {
                    errors.rejectValue 'platform', 'valueShouldBeConstant',
                            ['platform', rows[0].studyId, target.studyId] as Object[], null
                }
            }

            /* platform should be uppercase */
            if (target.platform.toUpperCase(LocaleContextHolder.locale) != target.platform) {
                errors.rejectValue 'platform', 'expectedUppercase',
                        [target.platform] as Object[], null
            }

            if (target.studyId != studyId) {
                errors.rejectValue 'studyId', 'expectedConstant',
                        [studyId, target.studyId] as Object[], null
            }
        }
    }


}
