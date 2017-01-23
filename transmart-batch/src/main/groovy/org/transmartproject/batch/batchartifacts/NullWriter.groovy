package org.transmartproject.batch.batchartifacts

import org.springframework.batch.item.ItemWriter

/**
 * Empty writer implementation.
 */
class NullWriter implements ItemWriter<Object> {

    @Override
    void write(List<?> items) throws Exception {
        // intentionally left blank
    }
}
