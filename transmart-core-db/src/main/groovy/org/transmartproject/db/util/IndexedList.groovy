package org.transmartproject.db.util

import groovy.transform.CompileStatic
import org.transmartproject.core.exceptions.InvalidArgumentsException

import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * An ArrayList that keeps an index of its content so that contains()/indexOf() are fast. This implementation
 * requires uniqueness of its contents, so it also works like an ordered set.
 */
@CompileStatic
class IndexedList<E> extends ArrayList<E> {
    private HashMap<E, Integer> indexMap = new HashMap()

    private void reindex() {
        indexMap.clear()
        int idx = 0
        this.each { addToIndex(it, idx++) }
    }

    private E addToIndex(E e, int idx) {
        if (indexMap.putIfAbsent(Objects.requireNonNull(e), idx) != null) duplicateError(e)
        e
    }

    private E checkDuplicates(E e) {
        if (indexMap.containsKey(Objects.requireNonNull(e))) duplicateError(e)
        e
    }

    static private void duplicateError(E e) {
        throw new InvalidArgumentsException("IndexedList cannot contain duplicate elements, '$e' is already present.")
    }

    @Override
    boolean add(E e) {
        super.add(addToIndex(e, size()))
        true
    }

    @Override
    boolean addAll(Collection<? extends E> c) {
        c.each { add(it) }
        c.size() != 0
    }

    @Override
    boolean contains(Object e) {
        indexMap.containsKey(e)
    }

    @Override
    int indexOf(Object e) {
        if (e == null) return -1
        indexMap[(E) e] ?: -1
    }

    @Override
    int lastIndexOf(Object e) {
        indexOf(e)
    }

    @Override
    E set(int idx, E e) {
        Integer oldidx = indexMap.putIfAbsent(Objects.requireNonNull(e), idx)
        if (oldidx != null && oldidx != idx) duplicateError(e)
        E rv = super.set(idx, e)
        if (oldidx == null) indexMap.remove(rv)
        rv
    }

    @Override
    void add(int idx, E e) {
        super.add(idx, checkDuplicates(e))
        reindex()
    }

    @Override
    boolean remove(Object e) {
        boolean rv = super.remove(e)
        reindex()
        rv
    }

    @Override
    E remove(int idx) {
        E rv = super.remove(idx)
        reindex()
        rv
    }

    @Override
    void clear() {
        super.clear()
        indexMap.clear()
    }

    @Override
    boolean addAll(int idx, Collection<? extends E> c) {
        c.each { addToIndex(it, -1) }
        boolean rv = super.addAll(idx, c)
        reindex()
        rv
    }

    @Override
    boolean removeAll(Collection<?> c) {
        boolean rv = super.removeAll(c)
        reindex()
        rv
    }

    @Override
    boolean retainAll(Collection<?> c) {
        boolean rv = super.retainAll(c)
        reindex()
        rv
    }

    @Override
    boolean removeIf(Predicate<? super E> filter) {
        boolean rv = super.removeIf(filter)
        reindex()
        rv
    }

    @Override
    void replaceAll(UnaryOperator<E> operator) {
        indexMap.clear()
        try {
            this.eachWithIndex { E e, int idx ->
                E newval = operator.apply(e)
                addToIndex(newval, idx)
                super.set(idx, newval)
            }
        } catch (Exception ex) {
            // If there's an exception the indexMap will be inconsistent
            reindex()
            throw ex
        }
    }

    @Override
    void sort(Comparator<? super E> c) {
        super.sort(c)
        reindex()
    }
}
