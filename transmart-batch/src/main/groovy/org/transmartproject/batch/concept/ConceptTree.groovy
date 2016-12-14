package org.transmartproject.batch.concept

import com.google.common.base.Predicate
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.ontology.OntologyNode
import org.transmartproject.batch.clinical.variable.ClinicalVariable
import org.transmartproject.batch.db.SequenceReserver

import javax.annotation.PostConstruct

/**
 * Structure to hold all the concepts found in the database and data files related to a study
 */
@Component
@JobScope
@Slf4j
@CompileStatic
class ConceptTree {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    @Autowired
    private SequenceReserver reserver

    /* concept nodes indexed by path */
    private final NavigableMap<ConceptPath, ConceptNode> nodeMap =
            Maps.newTreeMap()

    private final Set<ConceptNode> savedNodes = []

    private final Set<String> savedConceptCodes = []

    /* concept nodes indexed by concept path */
    private final NavigableMap<ConceptPath, ConceptNode> conceptNodes =
            Maps.newTreeMap()

    @PostConstruct
    void generateStudyNode() {
        // automatically creates nodes up until topNodePath
        // if any of these already exist, they should be replaced in loadTreeNodes()
        getOrGenerate(topNodePath, null, ConceptType.UNKNOWN) // for collaterals
    }

    void loadExisting(Collection<ConceptNode> nodes) {
        nodes.each { n ->
            if (log.traceEnabled &&
                    nodeMap.containsKey(n.path)) {
                log.trace "Replacing ${nodeMap[n.path]} with $n"
            }

            nodeMap[n.path] = n
            if (n.conceptPath) {
                conceptNodes[n.conceptPath] = n
            }
            savedNodes << n
        }
    }

    void addToSavedConceptCodes(Collection<String> codes) {
        this.savedConceptCodes.addAll(codes)
    }

    Set<String> getSavedConceptCodes() {
        savedConceptCodes
    }

    Set<ConceptNode> getNewConceptNodes() {
        Sets.difference(allConceptNodes, savedNodes)
    }

    Set<ConceptNode> getAllConceptNodes() {
        nodeMap.values().toSet()
    }

    ConceptNode parentFor(ConceptNode child) {
        def parentPath = child.path.parent
        if (!parentPath) {
            return null
        }

        def result = nodeMap[parentPath]
        if (!result) {
            throw new IllegalStateException(
                    "Parent of $child is not known in the concept tree. " +
                            "Node that loadAll() must be given all the parents " +
                            "of the added nodes")
        }

        result
    }

    ConceptNode getAt(ConceptPath path) {
        nodeMap[path]
    }

    ConceptNode conceptNodeForConceptPath(ConceptPath conceptPath) {
        conceptNodes[conceptPath]
    }

    ConceptNode getOrGenerateConceptForVariable(ClinicalVariable variable) {
        assert variable && variable.conceptPath && variable.path
        def node = conceptNodes[variable.conceptPath]
        if (node) {
            return node
        }
        node = new ConceptNode(variable.path)
        node.type = ClinicalVariable.conceptTypeFor(variable)
        node.conceptName = variable.conceptName
        node.conceptPath = variable.conceptPath
        node.code = variable.conceptCode
        log.debug("Generated new concept node: ${node.conceptPath}")
        nodeMap[variable.path] = node
        if (node.conceptPath) {
            conceptNodes[node.conceptPath] = node
        }
        node
    }

    /**
     * Creates a concept node representing both an i2b2 ontology node and a concept in the concept dimension.
     *
     * @param path The path in the i2b2 ontology tree.
     * @param variable The variable represented by the node.
     * @param type (optional) overrides the concept type specifier in the variable.
     * @return the generated node.
     */
    ConceptNode getOrGenerate(ConceptPath path, ClinicalVariable variable, ConceptType type = null) {
        // choose specified type if available, else choose type based on the variable
        def conceptType = type ?: ClinicalVariable.conceptTypeFor(variable)
        def node = nodeMap[path]
        if (node) {
            if (node.type == ConceptType.UNKNOWN) {
                node.type = conceptType
                log.debug("Assigning type $conceptType to concept node $node")
            } else if (conceptType != ConceptType.UNKNOWN && node.type != conceptType) {
                throw new UnexpectedConceptTypeException(conceptType, node.type, path)
            }
            return node
        }

        for (def p = path.parent; p != null; p = p.parent) {
            getOrGenerate(p, null, ConceptType.UNKNOWN) /* for the collaterals */
        }

        node = new ConceptNode(path)
        node.type = conceptType
        if (node.type in [ConceptType.CATEGORICAL, ConceptType.HIGH_DIMENSIONAL]) {
            node.conceptName = path[-1]
            node.conceptPath = path
        } else if (variable) {
            node.conceptName = variable.conceptName
            node.conceptPath = variable.conceptPath
            node.code = variable.conceptCode
        }
        log.debug("Generated new concept node: $path")
        nodeMap[path] = node
        if (node.conceptPath) {
            conceptNodes[node.conceptPath] = node
        }
        node
    }

    ConceptNode getOrGenerateNode(ConceptPath path, OntologyNode ontologyNode) {
        def node = nodeMap[path]
        if (node) {
            return node
        }

        node = new ConceptNode(path)
        node.ontologyNode = true
        node.name = ontologyNode.label
        node.conceptPath = new ConceptPath(['Ontology', ontologyNode.code])
        node.code = ontologyNode.code
        node.conceptName = ontologyNode.label
        node.uri = ontologyNode.uri
        log.debug("Generated new node: $path")
        nodeMap[path] = node
        node
    }

    Collection<ConceptNode> childrenFor(ConceptNode parent) {
        /* String comparison compares the UTF-16 code units, so we give
         * U+00FFFF as the upper bound to get everything that starts
         * with parent.path */
        nodeMap.subMap(parent.path, false,
                new ConceptPath(parent.path + ((char) 0xFFFF.byteValue()).toString()), false)
                .values()
    }

    boolean isSavedNode(ConceptNode node) {
        node in savedNodes
    }

    void reserveIdsFor(ConceptNode node) {
        if (!node.conceptPath) {
            log.debug "No concept path set for node ${node.path}, not reserving a concept id."
            return
        }
        if (node.code) {
            log.debug "Code already set for ${node.conceptPath}, not reserving a concept id."
            return
        }
        if (isSavedNode(node)) {
            log.warn("Could not rewrite id for node that already has one (${node.code}).")
            return
        }

        node.code = reserver.getNext(Sequences.CONCEPT)
    }

    void reserveIdsFor(Collection<ConceptNode> nodes) {
        nodes.each { reserveIdsFor(it) }
    }

    void addToSavedNodes(Collection<ConceptNode> nodes) {
        savedNodes.addAll(nodes)
    }

    boolean isStudyNode(ConceptNode node) {
        topNodePath.isPrefixOf(node.path)
    }

    Collection<ConceptNode> getAllStudyNodes() {
        Sets.filter(savedNodes, { ConceptNode it ->
            isStudyNode(it)
        } as Predicate<ConceptNode>)
    }
}
