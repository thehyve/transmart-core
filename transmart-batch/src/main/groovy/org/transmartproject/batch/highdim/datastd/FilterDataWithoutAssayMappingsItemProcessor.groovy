package org.transmartproject.batch.highdim.datastd

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.highdim.assays.AssayMappingsRowStore
import org.transmartproject.batch.highdim.assays.MappingFileRow

/**
 * Filters out items (when the flag is on {@link this.skipUnmappedData}) that do not have assay mappings.
 */
class FilterDataWithoutAssayMappingsItemProcessor
        implements ItemProcessor<PatientInjectionSupport, PatientInjectionSupport> {

    @Autowired
    private AssayMappingsRowStore assayMappings

    @Override
    PatientInjectionSupport process(PatientInjectionSupport item) throws Exception {
        MappingFileRow mappingFileRow = assayMappings.getBySampleName(item.sampleCode)
        if (mappingFileRow) {
            item
        }
    }

}
