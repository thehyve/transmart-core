package org.transmartproject.batch.support

import com.google.common.collect.AbstractIterator

/**
 *
 */
public interface SequenceReserver {

    Long getNext(String sequence)

    SequenceValues createBlock(String sequence)

    void configureBlockSize(String sequence, long blockSize)

    void setDefaultBlockSize(long blockSize)

}

class SequenceValues extends AbstractIterator<Long> {
    LinkedList<Long> ids

    protected Long computeNext() {
        if (ids.empty) {
            return endOfData()
        }

        ids.pop()
    }
}
