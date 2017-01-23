package org.transmartproject.batch.db

import com.google.common.collect.AbstractIterator
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch

/**
 * Component that reserves blocks of ids from database sequences and hands them
 * out one at a time.
 */
@Slf4j
abstract class SequenceReserver {

    private final Map<String, Long> blockSizes = [:].asSynchronized()

    long defaultBlockSize = 50

    @Autowired
    private ApplicationContext applicationContext

    @Autowired
    protected NamedParameterJdbcTemplate template

    private final ConcurrentMap<String, Object> blocks = new ConcurrentHashMap()

    abstract List<Long> getValuesFromDatabase(String sequence, long blockSize)

    // not part of the API
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SequenceValues reserveBlock(String sequence) {
        Long blockSize = blockSizes[sequence.toLowerCase()] ?: defaultBlockSize
        List<Long> list = getValuesFromDatabase(sequence, blockSize)
        new SequenceValues(ids: new LinkedList(list))
    }

    void configureBlockSize(String sequence, long blockSize) {
        blockSizes[sequence.toLowerCase()] = blockSize
    }

    Long getNext(String sequence) {
        Object block = blocks.get(sequence)
        if (block instanceof CountDownLatch) {
            block.await()
            return getNext(sequence)
        } else if (block == null) {
            def latch = new CountDownLatch(1)
            if (blocks.putIfAbsent(sequence, latch) != null) {
                // already something there
                return getNext(sequence)
            }
            try {
                // get the bean from the app context so we get it decorated
                block = applicationContext.getBean(SequenceReserver).reserveBlock(sequence)
                def ret = block.next()
                blocks.put sequence, block
                return ret
            } finally {
                latch.countDown()
            }
        } else {
            assert block instanceof SequenceValues
            synchronized (block) {
                if (!block.hasNext()) {
                    blocks.remove(sequence, block)
                } else {
                    return block.next()
                }
            }
            return getNext(sequence)
        }
    }
}


class SequenceValues extends AbstractIterator<Long> {
    Deque<Long> ids

    protected Long computeNext() {
        if (ids.empty) {
            return endOfData()
        }

        ids.pop()
    }
}
