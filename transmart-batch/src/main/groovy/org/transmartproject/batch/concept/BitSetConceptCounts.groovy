package org.transmartproject.batch.concept

import com.google.common.collect.Maps
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.patient.Patient

/**
 * Uses a bitsets associated with the concept nodes to calculate concept counts.
 * This can only be used if the data is being inserted from scratch for the
 * study in question.
 */
@Slf4j
class BitSetConceptCounts {

    @Autowired
    ConceptTree conceptTree

    private int patientCounter

    private final Map<Patient, Integer> patientIndexes =
            Maps.newHashMap().withDefault { patientCounter++ }

    private final Map<ConceptNode, BitSet> conceptBitSets =
            Maps.newHashMap().withDefault { new BitSet() }

    void registerObservation(Patient patient, ConceptNode conceptNode) {
        conceptBitSets[conceptNode].set(patientIndexes[patient], true)
    }

    int getConceptCount(ConceptNode node) {
        def allNodes = [node] + conceptTree.childrenFor(node)
        allNodes
                .collect { conceptBitSets[it] }
                .inject(new BitSet()) { acc, val -> acc | val }
                .cardinality()
    }
}
