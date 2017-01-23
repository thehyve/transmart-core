package org.transmartproject.batch.highdim.metabolomics.platform

import com.google.common.collect.Sets
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.validator.ValidationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.highdim.metabolomics.platform.model.Biochemical
import org.transmartproject.batch.highdim.metabolomics.platform.model.SubPathway
import org.transmartproject.batch.highdim.metabolomics.platform.model.SuperPathway

/**
 * Holds the biochemicals, sub and super pathways in memory.
 * Also does some validation.
 */
@Component
@JobScope
class MetabolomicsBiochemicalsPile {

    @Autowired
    SequenceReserver sequenceReserver

    SortedSet<Biochemical>  biochemicals  = Sets.newTreeSet()
    SortedSet<SubPathway>   subPathways   = Sets.newTreeSet()
    SortedSet<SuperPathway> superPathways = Sets.newTreeSet()

    void leftShift(MetabolomicsAnnotationRow row) {
        def superPathway
        if (row.superPathway) {
            superPathway = new SuperPathway(name: row.superPathway)
            if (superPathways.contains(superPathway)) {
                // use old one, there may be references to
                superPathway = superPathways.tailSet(superPathway).first()
                assert superPathway != null
            } else {
                superPathways << superPathway
            }
        }

        def subPathway
        if (row.subPathway) {
            subPathway = new SubPathway(
                    name: row.subPathway,
                    superPathway: superPathway)
            if (subPathways.contains(subPathway)) {
                SubPathway currentSubPathway =
                        subPathways.tailSet(subPathway).first()
                assert currentSubPathway != null
                validateSuperPathwayOfSubPathway(subPathway, currentSubPathway)
                // use old one, there may be references to it
                subPathway = currentSubPathway
            } else {
                subPathways << subPathway
            }
        }

        def biochemical = new Biochemical(
                name: row.biochemical,
                hmdbId: row.hmdbId,
                subPathway: subPathway)
        boolean success = biochemicals << biochemical
        assert success // should not be there already
    }

    long assignIds() {
        biochemicals.each {
            it.id = sequenceReserver.getNext(Sequences.METAB_ANNOT_ID)
        }
        subPathways.each {
            it.id = sequenceReserver.getNext(Sequences.METAB_SUB_PATHWAY_ID)
        }
        superPathways.each {
            it.id = sequenceReserver.getNext(Sequences.METAB_SUPER_PATHWAY_ID)
        }

        biochemicals.size() + subPathways.size() + superPathways.size()
    }

    void validateSuperPathwayOfSubPathway(SubPathway newSubPathway,
                                          SubPathway oldSubPathway) {
        if (!Objects.equals(newSubPathway.superPathway,
                oldSubPathway.superPathway)) {
            throw new ValidationException("Mismatched super pathway for " +
                    "sub pathway $newSubPathway.name. Provided " +
                    "'${newSubPathway?.superPathway?.name}' while previously " +
                    "we had seen '${oldSubPathway?.superPathway?.name}'")
        }
    }
}
