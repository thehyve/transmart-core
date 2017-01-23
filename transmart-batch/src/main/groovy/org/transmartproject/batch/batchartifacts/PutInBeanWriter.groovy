package org.transmartproject.batch.batchartifacts

import org.springframework.batch.item.ItemWriter

/**
 * Simple writes that just adds items into a bean.
 */
class PutInBeanWriter<T> implements ItemWriter<T> {

    Object bean

    @Override
    void write(List<? extends T> items) throws Exception {
        items.each {
            bean << it
        }
    }
}
