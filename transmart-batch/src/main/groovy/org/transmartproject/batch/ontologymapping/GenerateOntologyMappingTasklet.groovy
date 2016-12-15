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
import org.transmartproject.batch.clinical.ontology.OntologyNode
import org.transmartproject.batch.clinical.variable.ClinicalVariable

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

    def ontologyMap = [
            (['Vital Signs', 'Weight_KG']): [[
                    code:       'SNOMEDCT/425024002',
                    label:      'Body weight without shoes',
                    uri:        'http://purl.bioontology.org/ontology/SNOMEDCT/425024002',
                    ancestors:  ['SNOMEDCT/27113001']]],
            (['Clinical Chemistry', 'HDL mg/dl']): [[
                    code:       'SNOMEDCT/17888004',
                    label:      'High density lipoprotein measurement',
                    uri:        'http://purl.bioontology.org/ontology/SNOMEDCT/17888004',
                    ancestors:  ['SNOMEDCT/104789001']]]
    ]

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info "Fetching ontology nodes from ${ontologyServerUrl}..."
        ontologyMappingHolder.nodes = []
        ctx.variables.each { ClinicalVariable variable ->
            log.info "Fetching for variable ${variable.dataLabel}..."
            def mappingResults = ontologyMap[[variable.categoryCode, variable.dataLabel]]
            if (mappingResults) {
                log.info "Found mapping for ${variable.dataLabel}!"
                mappingResults.each { mapping ->
                    ontologyMappingHolder.nodes << new OntologyNode(
                            categoryCode: variable.categoryCode,
                            dataLabel: variable.dataLabel,
                            code: mapping.code,
                            label: mapping.label,
                            uri: mapping.uri,
                            ancestorCodes: mapping.ancestors
                    )
                    contribution.incrementReadCount()
                }
            }
        }

        RepeatStatus.FINISHED
    }
}
