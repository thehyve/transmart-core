package org.transmartproject.batch.highdim.mirna.platform

import org.springframework.batch.item.ItemProcessor

/**
 * Lower case {@link MirnaAnnotationRow#mirnaId}
 */
class LowerCaseMirnaIdItemProcessor implements ItemProcessor<MirnaAnnotationRow, MirnaAnnotationRow> {
    @Override
    MirnaAnnotationRow process(MirnaAnnotationRow item) throws Exception {
        item.mirnaId = item.mirnaId?.toLowerCase()

        item
    }
}
