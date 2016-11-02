package org.transmartproject.db.util;

import org.transmartproject.core.exceptions.InvalidArgumentsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import static java.util.Objects.requireNonNull;

/**
 * An ArrayList that keeps an index of its content so that contains()/indexOf() are fast. This implementation
 * requires uniqueness of its contents, so it also works like an ordered set.
 */
public class IndexedList<E> extends ArrayList<E> {

    private HashMap<E, Integer> indexMap = new HashMap<>();

    private void reindex() {
        indexMap.clear();
        int idx = 0;
        for (E item: this) {
            addToIndex(item, idx++);
        }
    }

    private E addToIndex(E e, int idx) {
        if (indexMap.putIfAbsent(requireNonNull(e), idx) != null) duplicateError(e);
        return e;
    }

    private E checkDuplicates(E e) {
        if (indexMap.containsKey(requireNonNull(e))) duplicateError(e);
        return e;
    }

    private void duplicateError(E e) {
        throw new InvalidArgumentsException("IndexedList cannot contain duplicate elements, '"+e+"' is already present.");
    }

    @Override
    public boolean add(E e) {
        super.add(addToIndex(e, size()));
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E item: c) {
            add(item);
        }
        return c.size() != 0;
    }

    @Override
    public boolean contains(Object e) {
        return indexMap.containsKey(e);
    }

    @Override
    public int indexOf(Object e) {
        if (e == null) return -1;
        Integer i = indexMap.get(e);
        return (i == null) ? -1 : i;
    }

    @Override
    public int lastIndexOf(Object e) {
        return indexOf(e);
    }

    @Override
    public void add(int idx, E e) {
        super.add(idx, checkDuplicates(e));
        reindex();
    }

    @Override
    public boolean remove(Object e) {
        boolean rv;
        try { rv = super.remove(e); }
        finally { reindex(); }
        return rv;
    }

    @Override
    public void clear() {
        super.clear();
        indexMap.clear();
    }

    @Override
    public boolean addAll(int idx, Collection<? extends E> c) {
        boolean rv;
        try {
            for(E item : c) {
                // check uniqueness
                addToIndex(item, -1);
            }
            rv = super.addAll(idx, c);
        } finally {
            reindex();
        }
        return rv;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean rv;
        try { rv = super.removeAll(c); }
        finally { reindex(); }
        return rv;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean rv;
        try { rv = super.retainAll(c); }
        finally { reindex(); }
        return rv;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        boolean rv;
        try { rv = super.removeIf(filter); }
        finally { reindex(); }
        return rv;
    }

    @Override
    public void replaceAll(final UnaryOperator<E> operator) {
        indexMap.clear();
        try {
            for (int i = 0; i < size(); i++) {
                E newval = operator.apply(this.get(i));
                addToIndex(newval, i);
                IndexedList.super.set(i, newval);
            }
        } catch (Exception ex) {
            // If there's an exception the indexMap will be inconsistent
            reindex();
            throw ex;
        }

    }

    @Override
    public void sort(Comparator<? super E> c) {
        try { super.sort(c); }
        finally { reindex(); }
    }
}
