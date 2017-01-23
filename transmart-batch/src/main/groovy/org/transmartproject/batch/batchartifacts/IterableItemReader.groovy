package org.transmartproject.batch.batchartifacts

import groovy.transform.CompileStatic
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.expression.EvaluationException
import org.springframework.stereotype.Component
import org.transmartproject.batch.support.ExpressionResolver

/**
 * Item reader that gets its items from an {@link Iterable} object.
 * This iterable object can either be assigned directly or through an expression.
 */
@CompileStatic
@Component
class IterableItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

    Iterable<T> iterable

    String expression

    @Autowired
    ExpressionResolver expressionResolver

    private Iterator<T> iterator

    @Override
    protected T doRead() throws Exception {
        if (iterator.hasNext()) {
            iterator.next()
        } // else null
    }

    @Override
    protected void doOpen() throws Exception {
        fetchIterator()
    }

    private Iterator<T> fetchIterator() {
        if (iterable == null) {
            iterable = expressionResolver.resolve(expression, Iterable)
            if (iterable == null) {
                throw new EvaluationException(
                        "Expression '$expression' yielded a null")
            }
        }

        iterator = iterable.iterator()
    }

    @Override
    protected void doClose() throws Exception {
        if (iterator instanceof Closeable) {
            ((Closeable) iterator).close()
        }
    }
}
