package org.transmartproject.batch.ontologymapping

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.ClinicalJobContext
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.ontology.DefaultExternalOntologyTermService
import org.transmartproject.ontology.ExternalOntologyTermService

/**
 * Generates an ontology mapping by fetching ontology information
 * from an ontology server.
 */
@Component
@JobScope
@Slf4j
class GenerateOntologyMappingTasklet implements Tasklet {

    @Autowired
    ClinicalJobContext ctx

    @Autowired
    OntologyMappingHolder ontologyMappingHolder

    @Value("#{jobParameters['ONTOLOGY_SERVER_URL']}")
    String ontologyServerUrl

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info "Fetching ontology nodes from ${ontologyServerUrl}..."
        ExternalOntologyTermService service = new DefaultExternalOntologyTermService(ontologyServerUrl)
        ontologyMappingHolder.nodes = [:]
        ctx.variables.each { ClinicalVariable variable ->
            log.debug "Fetching for variable ${variable.dataLabel}..."
            def mappingResults = service.fetchPreferredConcept(variable.categoryCode, variable.dataLabel)
            if (mappingResults) {
                mappingResults.each { mapping ->
                    if (mapping.dataLabel) {
                        log.info "Found ontology mapping for variable ${variable.dataLabel}: " +
                                "${mapping.label} (${mapping.ontologyCode})."
                    }
                    def key = new OntologyMappingHolder.Key(
                            ontologyCode: mapping.ontologyCode,
                            categoryCode: mapping.categoryCode,
                            dataLabel: mapping.dataLabel
                    )
                    if (!ontologyMappingHolder.nodes.containsKey(key)) {
                        ontologyMappingHolder.nodes[key] = mapping
                        contribution.incrementReadCount()
                    }
                }
            }
        }

        RepeatStatus.FINISHED
    }
}
