package org.transmartproject.batch.gwas.metadata

import groovy.util.logging.Slf4j
import org.springframework.batch.core.partition.support.Partitioner
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Creates the partitions for splitting the flow of the job for each analysis
 */
@Component
@Slf4j
class GwasAnalysisPartitioner implements Partitioner {

    public static final String KEY_FOR_PARTITION_ENTRY_INDEX = 'partitionEntryIndex'

    private static final String PARTITION_NAME_PREFIX = 'gwas_analysys_partition_'

    @Autowired
    private GwasMetadataStore gwasMetadataStore

    @Override
    Map<String, ExecutionContext> partition(int gridSize) {
        def res = (1..(gwasMetadataStore.size)).collectEntries {
            def ctx = new ExecutionContext()
            ctx.put(KEY_FOR_PARTITION_ENTRY_INDEX, it - 1)
            // this execution context pair will be bound to the step context
            [PARTITION_NAME_PREFIX + (it as String), ctx]
        }
        log.debug("Partitions are: $res")
        res
    }
}
