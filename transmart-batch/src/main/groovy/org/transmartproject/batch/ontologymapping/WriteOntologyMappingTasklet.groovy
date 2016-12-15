package org.transmartproject.batch.ontologymapping

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Component

/**
 * Writes ontology nodes to a ontology mapping file.
 */
@Component
@JobScope
@Slf4j
class WriteOntologyMappingTasklet implements Tasklet {

    @Autowired
    OntologyMappingHolder ontologyMappingHolder

    private FileSystemResource resource

    WriteOntologyMappingTasklet(FileSystemResource resource) {
        this.resource = resource
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!ontologyMappingHolder.nodes) {
            RepeatStatus.FINISHED
        }
        resource.outputStream.withPrintWriter { Writer w ->
            ontologyMappingHolder.nodes.each {
                w.write(it.code + '\n')
                contribution.incrementWriteCount(1)
            }
        }

        RepeatStatus.FINISHED
    }
}
