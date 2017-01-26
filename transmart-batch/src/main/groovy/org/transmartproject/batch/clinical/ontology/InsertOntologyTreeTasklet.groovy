package org.transmartproject.batch.clinical.ontology

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.ClinicalJobContext
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree

/**
 * Adds nodes to the ontology tree based on the variables
 * in the variable mapping and the ontology codes in the ontology
 * mapping file.
 */
@Component
@JobScope
@Slf4j
class InsertOntologyTreeTasklet implements Tasklet {

    @Autowired
    OntologyMapping ontologyMapping

    @Autowired
    ConceptTree conceptTree

    @Autowired
    ClinicalJobContext ctx

    private final Set<String> done = []

    private final Map<String, List<Entry>> entryMap = [:]

    class Entry {
        List<String> path
        OntologyNode node
    }

    private List<Entry> orderByDependencies(String code) {
        if (code in done) {
            def result = entryMap[code]
            if (!result) {
                log.error "Perhaps a cycle in the ontology mapping?"
                throw new RuntimeException("Unexpected state: node visited before, but no result available.")
            }
            return result
        }
        def node = ontologyMapping.nodes[code]
        if (!node) {
            throw new NodeNotFoundException("No node found for code ${code}")
        }
        log.debug "Traversing node ${node.label}..."
        done << code
        if (node.ancestorCodes.empty) {
            log.debug "No ancestors for node ${node.label}..."
            def result = [new Entry(node: node, path: [node.label])] as List<Entry>
            entryMap[code] = result
            log.debug "Adding node ${[node.label]}"
            return result
        }
        List<Entry> result = []
        node.ancestorCodes.each { String ancestorCode ->
            def dependencies = orderByDependencies(ancestorCode)
            result = dependencies
            dependencies.findAll { Entry ancestor ->
                ancestorCode == ancestor.node.code
            }.each { Entry ancestor ->
                result << ancestor
                result << new Entry(node: node, path: ancestor.path + [node.label])
                log.debug "Adding node ${ancestor.path + [node.label]}"
            }
        }
        result = result.unique {
            it.path
        }
        entryMap[code] = result
        result
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        def ontologyCodes = ontologyMapping.nodes.keySet() - conceptTree.savedConceptCodes
        def ontologyCodesInOrder = ontologyCodes.collectMany {
            orderByDependencies(it)
        }.unique {
            it.path
        }

        ontologyCodesInOrder.each { Entry entry ->
            log.debug "Generate entry '${entry.path}'..."
            // find variable for entry if present
            def var = ctx.variables.find {
                it.categoryCode == entry.node.categoryCode &&
                it.dataLabel == entry.node.dataLabel
            }
            // generate study specific node if it is a variable node
            if (var) {
                conceptTree.getOrGenerate(
                        new ConceptPath(var.path),
                        var
                )
            }
            // generate generic node
            conceptTree.getOrGenerateNode(
                    new ConceptPath(entry.path),
                    entry.node
            )
            contribution.incrementFilterCount(1)
        }
        RepeatStatus.FINISHED
    }
}
