package org.transmartproject.batch.concept

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
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

    private final Set<ConceptPath> oldConceptPaths =
            Sets.newTreeSet()

    @PostConstruct
    void generateStudyNode() {
        // automatically creates nodes up until topNodePath
        // if any of these already exist, they should be replaced in loadExisting()
        getOrGenerate(topNodePath, ConceptType.CATEGORICAL) // for collaterals
    }

    void loadExisting(Collection<ConceptNode> nodes) {
        nodes.each { n ->
            if (log.traceEnabled &&
                    nodeMap.containsKey(n.path)) {
                log.trace "Replacing ${nodeMap[n.path]} with $n"
            }

            nodeMap[n.path] = n
            oldConceptPaths << n.path
        }
    }

    boolean isNew(ConceptNode node) {
        !(node.path in oldConceptPaths)
    }

    Collection<ConceptNode> getAllConceptNodes() {
        nodeMap.values()
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

    ConceptNode getOrGenerate(ConceptPath path, ConceptType type) {
        def node = nodeMap[path]
        if (node) {
            if (node.type == ConceptType.UNKNOWN) {
                node.type = type
                log.debug("Assigning type $type to concept node $node")
            } else if (type != ConceptType.UNKNOWN && node.type != type) {
                throw new UnexpectedConceptTypeException(type, node.type, path)
            }
            return node
        }

        for (def p = path.parent; p != null; p = p.parent) {
            getOrGenerate(p, ConceptType.CATEGORICAL) /* for the collaterals */
        }

        node = new ConceptNode(path)
        node.type = type
        log.debug("Generated new concept node: $path")
        nodeMap[path] = node
    }

    Collection<ConceptNode> childrenFor(ConceptNode parent) {
        /* String comparison compares the UTF-16 code units, so we give
         * U+00FFFF as the upper bound to get everything that starts
         * with parent.path */
        nodeMap.subMap(parent.path, false,
                new ConceptPath(parent.path + ((char) 0xFFFF).toString()), false)
                .values()
    }

    void reserveIdsFor(ConceptNode node) {
        if (node.insertable) {
            return
        }

        node.code = reserver.getNext(Sequences.CONCEPT)
        node.i2b2RecordId = reserver.getNext(Sequences.I2B2_RECORDID)
    }

    void reserveIds() {
        allConceptNodes.each { reserveIdsFor it }
    }

    boolean isStudyNode(ConceptNode node) {
        topNodePath.isPrefixOf(node.path)
    }

    Collection<ConceptNode> getNewConceptNodes() {
        allConceptNodes.findAll {
            !(it.path in oldConceptPaths)
        }
    }
}
