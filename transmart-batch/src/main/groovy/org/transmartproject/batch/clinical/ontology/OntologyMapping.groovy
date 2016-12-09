package org.transmartproject.batch.clinical.ontology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptPath

import javax.annotation.PostConstruct

/**
 * Structure to hold the ontology mapping that maps variables to ontology terms
 * and the ancestry of ontology terms.
 */
@Component
@JobScope
@Slf4j
@CompileStatic
class OntologyMapping {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    /**
     * The map from ontology code to ontology node
     */
    final Map<String, OntologyNode> nodes = [:]

    /**
     * The map from concept path (category code and data label), representing
     * a variable, to the ontology node associated with it.
     */
    final Map<ConceptPath, OntologyNode> nodeMap = [:]

    void leftShift(OntologyNode node) {
        add(node)
    }

    void add(OntologyNode node) {
        // add node to nodes
        if (!(node.code in nodes.keySet())) {
            nodes.put(node.code, node)
        }
        // if the node contains category code and data label, add to the node map
        def parts = [node.categoryCode, node.dataLabel].findAll{ it != null && !it.empty }
        if (!parts.empty) {
            ConceptPath path = topNodePath
            parts.each {
                path += ConceptFragment.decode(it)
            }
            nodeMap[path] = nodes[node.code]
        }
    }

}
