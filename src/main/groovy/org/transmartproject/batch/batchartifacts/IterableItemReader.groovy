package org.transmartproject.batch.batchartifacts

import groovy.transform.CompileStatic
import org.springframework.batch.item.ItemReader
import org.springframework.expression.EvaluationException
import org.transmartproject.batch.support.ExpressionResolver

/**
 * Item reader that gets its items from an {@link Iterable} object.
 * This iterable object can either be assigned directly or through an expression.
 */
@CompileStatic
class IterableItemReader<T> implements ItemReader<T>{

    Iterable<T> iterable

    String expression

    ExpressionResolver expressionResolver

    private Iterator<T> iterator

    @Override
    T read() {
        if (iterator == null) {
            iterator = fetchIterator()
        }

        if (iterator.hasNext()) {
            return iterator.next()
        } // else null
    }

    private Iterator<T> fetchIterator() {
        if (iterable == null) {
            iterable = expressionResolver.resolve(expression, Iterable)
            if (iterable == null) {
                throw new EvaluationException(
                        "Expression '$expression' yold a null")
            }
        }

        iterable.iterator()
    }
}
