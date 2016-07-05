package org.transmartproject.core

/**
 * The result of a query that provides a list of results in the form of an iterable.
 *
 * In general, the iterator can be fetched only once.
 *
 * @param < I > The type of the results themselves
 */
interface IterableResult<I> extends Closeable, Iterable<I> {}
