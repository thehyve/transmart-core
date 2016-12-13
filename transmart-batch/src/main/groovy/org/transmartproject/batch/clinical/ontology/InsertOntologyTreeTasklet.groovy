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

    class Entry {
        List<String> path
        OntologyNode node
    }

    private List<Entry> orderByDependencies(String code) {
        if (code in done) {
            return []
        }
        def node = ontologyMapping.nodes[code]
        if (!node) {
            throw new NodeNotFoundException("No node found for code ${code}")
        }
        if (node.ancestorCodes.empty) {
            done << code
            return [new Entry(node: node, path: [node.label])]
        }
        done << code
        List<Entry> result = []
        node.ancestorCodes.each { String ancestorCode ->
            def dependencies = orderByDependencies(ancestorCode)
            dependencies.each { Entry ancestor ->
                result << ancestor
                result << new Entry(node: node, path: ancestor.path + [node.label])
            }
        }
        result
    }

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        def ontologyCodes = ontologyMapping.nodes.keySet() - conceptTree.savedConceptCodes
        def ontologyCodesInOrder = ontologyCodes.collectMany {
            log.info "Considering ontology code '$it'..."
            orderByDependencies(it)
        }

        ontologyCodesInOrder.each { Entry entry ->
            // find variable for entry if present
            def var = ctx.variables.find {
                it.categoryCode == entry.node.categoryCode &&
                it.dataLabel == entry.node.dataLabel
            }
            log.info "Adding node for entry ${entry.path} (var = ${var?.conceptPath})"
            if (var) {
                conceptTree.getOrGenerate(
                        new ConceptPath(entry.path),
                        var
                )
            } else {
                conceptTree.getOrGenerateNode(
                        new ConceptPath(entry.path),
                        entry.node
                )
            }
            contribution.incrementFilterCount(1)
        }
        RepeatStatus.FINISHED
    }
}
