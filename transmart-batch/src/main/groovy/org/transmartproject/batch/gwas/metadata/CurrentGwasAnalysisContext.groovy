package org.transmartproject.batch.gwas.metadata

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.springframework.batch.core.annotation.AfterWrite
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.StepSynchronizationManager
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap

/**
 * Context information for the analysis being loaded.
 */
@Component
@JobScope
@Slf4j
@ToString(includeNames = true)
class CurrentGwasAnalysisContext {

    private static final String CONTEXT_KEY = 'gwasAnalysisContext'

    private int getCurrentIndex() {
        def i = StepSynchronizationManager.context.stepExecutionContext[
                GwasAnalysisPartitioner.KEY_FOR_PARTITION_ENTRY_INDEX]
        if (i == null) {
            throw new IllegalStateException("No analysis in context")
        }
        i
    }

    Map<Integer, Long> bioAssayAnalysisIds = new ConcurrentHashMap<>()

    Map<Integer, Long> bioAssayAnalysisExtIds = new ConcurrentHashMap<>()

    Map<Integer, Long> analysesRows  = new ConcurrentHashMap<>()

    @Autowired
    private GwasMetadataStore metadataStore

    @Value('#{jobExecution.executionContext}')
    private ExecutionContext jobExecutionContext

    @PostConstruct
    void loadFromContext() {
        List<Object> storedState = jobExecutionContext.get(CONTEXT_KEY)
        if (storedState) {
            log.info("Restoring ${getClass().name} state")

            (
                    bioAssayAnalysisIds,
                    bioAssayAnalysisExtIds,
                    analysesRows
            ) = storedState

            log.info "Restored object is $this"
        }
    }

    GwasMetadataEntry getMetadataEntry() {
        metadataStore[currentIndex]
    }

    Long getBioAssayAnalysisId() {
        bioAssayAnalysisIds[currentIndex]
    }

    Long getAnalysisRowCount() {
        analysesRows[currentIndex]
    }

    void updateIds(long bioAssayAnalysisId, long bioAssayAnalysisExtId) {
        this.bioAssayAnalysisIds[currentIndex] = bioAssayAnalysisId
        this.bioAssayAnalysisExtIds[currentIndex] = bioAssayAnalysisExtId
    }

    class CurrentGwasAnalysisUpdateRowCountListener {
        @AfterWrite
        void afterWrite(List items) {
            def curValue = analysesRows[currentIndex] ?: 0
            analysesRows[currentIndex] = curValue + items.size()
            log.debug("Number of items written so far for " +
                    "analysis ${bioAssayAnalysisIds[currentIndex]}: " +
                    analysesRows[currentIndex])

            putInContext() // for restarts
        }
    }

    Object getUpdateRowCountListener() {
        new CurrentGwasAnalysisUpdateRowCountListener()
    }

    private void putInContext() {
        log.debug("Putting in context data for CurrentGwasAnalysisContext: $this")
        jobExecutionContext.put(CONTEXT_KEY,
                [bioAssayAnalysisIds,
                 bioAssayAnalysisExtIds,
                 analysesRows,
                ])
    }

}
