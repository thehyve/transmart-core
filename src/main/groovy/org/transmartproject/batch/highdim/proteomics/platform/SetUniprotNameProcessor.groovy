package org.transmartproject.batch.highdim.proteomics.platform

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.biodata.BioMarkerDictionary

/**
 * Set the uniprot name to the @link ProteomicsAnnotationRow interface.
 */
@Component
@Slf4j
class SetUniprotNameProcessor implements ItemProcessor<ProteomicsAnnotationRow, ProteomicsAnnotationRow> {

    @Autowired
    private BioMarkerDictionary bioMarkerDictionary

    @Override
    ProteomicsAnnotationRow process(ProteomicsAnnotationRow item) throws Exception {
        item.uniprotName = bioMarkerDictionary.getUniprotNameByUniporotId(item.uniprotId)

        if (!item.uniprotName) {
            log.warn("There is no protein with uniprot id ${item.uniprotId} found in the dictionary." +
                    " Fill in uniprot name field with uniprot id.")
            item.uniprotName = item.uniprotId
        }

        item
    }

}
