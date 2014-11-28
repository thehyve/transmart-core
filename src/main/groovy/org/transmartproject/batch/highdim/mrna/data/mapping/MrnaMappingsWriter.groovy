package org.transmartproject.batch.highdim.mrna.data.mapping

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.validator.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.transmartproject.batch.batchartifacts.MessageResolverSpringValidator
import org.transmartproject.batch.highdim.assays.MappingFileRow

import javax.annotation.Resource

/**
 * Validates and writes {@link MappingFileRow} files into memory.
 * We validate the items here because the validation depends on the value of the
 * first element. If this it was done in a processor and the chunk was larger
 * 1, then the process would have to kee state that's already kept in
 * MrnaMappings and is used by its inner validator class.
 */
@Component
@JobScope
class MrnaMappingsWriter implements ItemWriter<MappingFileRow> {

    @Autowired
    MrnaMappings mrnaMappings

    @Resource
    MessageSource validationMessageSource

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Validator<MappingFileRow> validator =
        new MessageResolverSpringValidator(
                mrnaMappings.validator, validationMessageSource)

    @Override
    void write(List<? extends MappingFileRow> items) throws Exception {
        items.each {
            validator.validate it
            mrnaMappings << it
        }
    }
}
