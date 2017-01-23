package org.transmartproject.batch.i2b2.mapping

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.i2b2.variable.I2b2Variable
import org.transmartproject.batch.i2b2.variable.I2b2VariableFactory

/**
 * Binds an {@link I2b2MappingEntry} to an {@link I2b2Variable}.
 */
@Component
@JobScopeInterfaced
class I2b2VariableBindingProcessor implements ItemProcessor<I2b2MappingEntry, I2b2MappingEntry> {

    @Autowired
    private I2b2VariableFactory i2b2VariableFactory

    private I2b2MappingEntry lastEntry

    @Override
    I2b2MappingEntry process(I2b2MappingEntry item) throws Exception {
        if (item.filename != lastEntry?.filename) {
            // if changing files, forget the last entry
            lastEntry = null
        }

        item.i2b2Variable = i2b2VariableFactory.create(
                item.variable, lastEntry?.i2b2Variable)

        lastEntry = item // return item
    }
}
