package org.transmartproject.batch.db

import com.google.common.collect.AbstractIterator
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

@Slf4j
class SequenceReserver {

    private Map<String, Long> blockSizes = [:].asSynchronized()
    static final String SQL = 'SELECT nextval(?) FROM generate_series(1, ?)'

    long defaultBlockSize = 50

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    JdbcTemplate template

    private ConcurrentHashMap<String, Object> blocks = new ConcurrentHashMap()

    // not part of the API
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    SequenceValues createBlock(String sequence) {
        Long blockSize = blockSizes[sequence] ?: defaultBlockSize
        List<Long> list = template.queryForList(SQL, [sequence, blockSize] as Object[], Long)
        new SequenceValues(ids: new LinkedList(list))
    }

    void configureBlockSize(String sequence, long blockSize) {
        blockSizes[sequence] = blockSize
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
                block = applicationContext.getBean(SequenceReserver).createBlock(sequence)
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
    LinkedList<Long> ids

    protected Long computeNext() {
        if (ids.empty) {
            return endOfData()
        }

        ids.pop()
    }
}
