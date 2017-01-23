package org.transmartproject.batch.highdim.assays

import org.springframework.batch.item.ItemProcessor
import org.springframework.context.i18n.LocaleContextHolder

/**
 * Uppercase {@link MappingFileRow#platform}
 */
class UpperCasePlatformIdItemProcessor implements ItemProcessor<MappingFileRow, MappingFileRow> {
    @Override
    MappingFileRow process(MappingFileRow item) throws Exception {
        item.platform = item.platform.toUpperCase(LocaleContextHolder.locale)
        item
    }
}
