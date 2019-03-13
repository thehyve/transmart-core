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
import org.transmartproject.ontology.OntologyMapTsvWriter

/**
 * Writes ontology nodes to a ontology mapping file.
 */
@Component
@JobScope
@Slf4j
class WriteOntologyMappingTasklet implements Tasklet {

    @Autowired
    OntologyMappingHolder ontologyMappingHolder

    private final FileSystemResource resource

    WriteOntologyMappingTasklet(FileSystemResource resource) {
        this.resource = resource
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (!ontologyMappingHolder.nodes) {
            RepeatStatus.FINISHED
        }

        def nodes = ontologyMappingHolder.nodes.values()
        log.info "Writing ${nodes.size()} ontology entries."

        def parentFile = resource.file.parentFile
        if (!parentFile.exists()) {
            log.info "Directory ${parentFile.path} does not exist. Creating..."
            parentFile.mkdirs()
        }
        OntologyMapTsvWriter.write(resource.outputStream, nodes)
        contribution.incrementWriteCount(ontologyMappingHolder.nodes.size())

        RepeatStatus.FINISHED
    }
}
