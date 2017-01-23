package org.transmartproject.batch.clinical.xtrial

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired

/**
 * CWriter for the <code>readXtrialsFileTasklet</code> tasket.
 * Merely register the mappings in the {@link XtrialMappingCollection}.
 */
class XtrialMappingWriter implements ItemWriter<XtrialMapping> {

    @Autowired
    XtrialMappingCollection mappingCollection

    @Override
    void write(List<? extends XtrialMapping> items) throws Exception {
        items.each {
            mappingCollection.registerUserMapping it
        }
    }
}
