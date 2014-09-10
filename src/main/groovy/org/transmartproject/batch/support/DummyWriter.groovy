package org.transmartproject.batch.support

import org.springframework.batch.item.ItemWriter

/**
 *
 */
class DummyWriter implements ItemWriter<Object> {

    @Override
    void write(List<?> items) throws Exception {
        println "${items.getClass()}:  $items"
    }

}
