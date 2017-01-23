package org.transmartproject.batch.gwas.metadata

import com.google.common.collect.Lists
import groovy.util.logging.Slf4j
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.AfterStep
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/**
 * The place where the {@link GwasMetadataEntry} objects are stored.
 */
@Component
@JobScope
@Slf4j
class GwasMetadataStore {

    private static final String CONTEXT_KEY = 'gwasMetaDataStoreEntries'

    private List<GwasMetadataEntry> entries = []

    @Value('#{jobExecution.executionContext}')
    private ExecutionContext jobExecutionContext

    @PostConstruct
    void restore() {
        entries = Lists.newArrayList(
                jobExecutionContext.get(CONTEXT_KEY) ?: Collections.emptyList())
    }

    void leftShift(GwasMetadataEntry entry) {
        log.debug("Added metadata entry: $entry")
        entries << entry
    }

    GwasMetadataEntry getAt(int i) {
        def res = entries[i]
        if (res == null) {
            throw new IndexOutOfBoundsException(
                    "No such analysis index: $i (only $size available)")
        }
        res
    }

    Set<String> getStudies() {
        entries*.study as Set
    }

    int getSize() {
        entries.size()
    }

    @AfterStep
    void registerInJobContext(StepExecution stepExecution) {
        if (stepExecution.exitStatus != ExitStatus.COMPLETED) {
            log.warn("Exit status was ${stepExecution.exitStatus}; " +
                    "nothing will be stored in the job context")
            return
        }
        jobExecutionContext.put(CONTEXT_KEY, Lists.newArrayList(entries))
    }
}
