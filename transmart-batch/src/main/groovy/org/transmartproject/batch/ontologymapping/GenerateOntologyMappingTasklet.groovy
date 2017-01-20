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
import org.transmartproject.ontology.ExternalOntologyTermService
import org.transmartproject.ontology.OntologyTermServiceRegistry

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

    @Value("#{jobParameters['ONTOLOGY_SERVICE_TYPE']}")
    String ontologyServiceType

    @Value("#{jobParameters['ONTOLOGY_SERVER_URL']}")
    String ontologyServerUrl

    @Value("#{jobParameters['ONTOLOGY_SERVER_SEARCH_PATH']}")
    String ontologyServerSearchPath

    @Value("#{jobParameters['ONTOLOGY_SERVER_DETAILS_PATH']}")
    String ontologyServerDetailsPath

    @Value("#{jobParameters['ONTOLOGY_SERVER_API_KEY']}")
    String ontologyServerApiKeyToken

    @Value("#{jobParameters['ONTOLOGY_SERVER_ONTOLOGIES']}")
    String ontologyServerOntologies

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        def serviceType = ontologyServiceType ?: OntologyTermServiceRegistry.instance.defaultServiceType
        def serviceParams = [
                'ontologyServerUrl',
                'ontologyServerSearchPath',
                'ontologyServerDetailsPath',
                'ontologyServerApiKeyToken',
                'ontologyServerOntologies'
        ].findAll { String name ->
            this."${name}"
        }.collectEntries { String name ->
            [(name): this."${name}"]
        }
        log.info "Fetching ontology nodes from ${ontologyServerUrl} ..."
        log.info "Ontology service type: ${serviceType}. Parameters: ${serviceParams.toMapString()}."
        ExternalOntologyTermService service = OntologyTermServiceRegistry.instance.create(
                serviceType, serviceParams
        )
        ontologyMappingHolder.nodes = [:]
        ctx.variables.each { ClinicalVariable variable ->
            log.debug "Fetching for variable ${variable.dataLabel} (${variable.categoryCode}) ..."
            def mappingResults = service.fetchPreferredConcept(variable.categoryCode, variable.dataLabel)
            if (mappingResults) {
                mappingResults.each { mapping ->
                    if (mapping.dataLabel == variable.dataLabel) {
                        log.info "Mapping variable ${variable.dataLabel} (${variable.categoryCode}) to: " +
                                "${mapping.label}."
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
