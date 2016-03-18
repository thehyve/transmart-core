package org.transmartproject.batch.highdim.assays

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.concept.ConceptPath

/**
 * Collects the {@link MappingFileRow}s.
 */
@Component
@JobScope
@Slf4j
class AssayMappingsRowStore implements MappingsFileRowStore {

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    private final List<MappingFileRow> rows = []

    @Override
    List<MappingFileRow> getRows() {
        this.@rows
    }

    void leftShift(MappingFileRow row) {
        rows << row
        log.debug "Added $row"
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
        rows.each {
            ret << topNode + it.conceptFragment
        }
        ret
    }

    String getPlatform() {
        if (!rows) {
            throw new IllegalStateException("No mapping rows read")
        }
        rows[0].platform
    }
}
