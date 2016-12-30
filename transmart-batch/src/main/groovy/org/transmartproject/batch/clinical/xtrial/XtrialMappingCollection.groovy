package org.transmartproject.batch.clinical.xtrial

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptType

/**
 * Collection of {@link XtrialMapping}s and {@link XtrialNode}s.
 * Able to map {@link ConceptNode}s to {@link XtrialNode}s.
 */
@Slf4j
@Component
@JobScope
class XtrialMappingCollection {

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptFragment topNode

    // indexed by path (minus leading \Across Trials)
    private final Map<ConceptFragment, XtrialNode> xtrialNodes = Maps.newHashMap()

    // order user mapping by study prefix
    private final NavigableMap<ConceptFragment, XtrialMapping> userMappings = Maps.newTreeMap()

    void registerUserMapping(XtrialMapping mapping) {
        userMappings[mapping.studyPrefixFragment] = mapping
    }

    void registerTrialNode(XtrialNode xtrialNode) {
        xtrialNodes[xtrialNode.path] = xtrialNode
    }

    XtrialNode findMappedXtrialNode(ConceptPath nodePath, ConceptType nodeType) {
        if (!topNode.isPrefixOf(nodePath)) {
            log.debug "Node $nodePath is outside of $topNode, no xtrial mapping possible"
            return null
        }

        ConceptFragment studyFragmentWithoutTopNode =
                nodePath.removePrefix(topNode)

        XtrialMapping xtrialMapping
        for (ConceptFragment f = studyFragmentWithoutTopNode; f != null; f = f.parent) {

            /* get the highest entry with key lower than or equal to f */
            def e = userMappings.headMap(f, true).descendingMap().firstEntry()
            if (e == null) {
                break; // shortcut; no chance of matches
            }

            XtrialMapping candidateMapping = e.value
            if (candidateMapping.studyPrefixFragment.isPrefixOf(f)) {
                xtrialMapping = candidateMapping
                break
            }
        }

        if (!xtrialMapping) {
            log.debug "No xtrial mapping for $studyFragmentWithoutTopNode"
            return null
        }

        log.debug "Node $studyFragmentWithoutTopNode matched user mapping $xtrialMapping"

        // remove the study prefix from the study concept and add it to the xtrial prefix
        def xtrialNodeFragment = xtrialMapping.xtrialPrefixFragment +
                studyFragmentWithoutTopNode.removePrefix(xtrialMapping.studyPrefixFragment)

        // try to find this xtrial concept
        def xtrialNode = xtrialNodes[xtrialNodeFragment]
        if (!xtrialNode) {
            log.warn("Node $studyFragmentWithoutTopNode matched mapping " +
                    "$xtrialMapping, but the xtrial concept $xtrialNodeFragment " +
                    'does not exist')
            return null
        }

        // found an xtrial node
        if (xtrialNode.type != nodeType) {
            log.error("Node $studyFragmentWithoutTopNode, matched mapping " +
                    "$xtrialMapping, but the xtrial concept " +
                    "$xtrialNodeFragment has type ${xtrialNode.type}, while " +
                    "the study concept has type ${nodeType}")
            return null
        }

        // all OK
        log.debug "Node $studyFragmentWithoutTopNode associated with xtrial node $xtrialNode"
        xtrialNode
    }

    List<ConceptFragment> getDisjunctXtrialPrefixes() {
        // all the xtrial concepts found in the registered mappings,
        // but excluding those concepts that are children of others
        SortedSet<ConceptFragment> concepts =
                Sets.newTreeSet(userMappings.values()*.xtrialPrefixFragment)

        concepts.inject([]) { List<ConceptFragment> accum, ConceptFragment cur ->
            if (!accum.empty && accum.last().isPrefixOf(cur)) {
                accum
            } else {
                accum + cur
            }
        }
    }
}
