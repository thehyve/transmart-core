package org.transmartproject.db.util;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import static java.util.Objects.requireNonNull;

/**
 * An ArrayList that keeps an index of its content so that contains()/indexOf() are fast. Duplicate entries are
 * ignored as most other java Set's do.
 */
public class IndexedArraySet<E> extends ArrayList<E> implements Set<E> {

    public IndexedArraySet() { super(); }

    public IndexedArraySet(Iterable<E> c) {
        super();
        addAll(c);
    }

    // Count to track the number of recursive calls that make the indexMap inconsistent
    private int inconsistent = 0;
    private HashMap<E, Integer> indexMap = new HashMap<>();

    private void invalidateIndex() {
        inconsistent++;
    }

    private void acceptIndex() {
        inconsistent--;
    }

    private void reindex() {
        indexMap.clear();
        int idx = 0;
        for (E item: this) {
            indexMap.putIfAbsent(requireNonNull(item), idx++);
        }
    }

    private void acceptReindex() {
        reindex();
        acceptIndex();
    }

    private void checkIndex() {
        if(inconsistent != 0) throw new IllegalStateException(
                "Recursive or parallel call of contains() or indexOf() while this object is being modified");
    }

    @Override
    public boolean add(E e) {
        if(indexMap.putIfAbsent(requireNonNull(e), size()) != null) return false;
        super.add(e);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll((Iterable<? extends E>) c);
    }
    public boolean addAll(Iterable<? extends E> c) {
        boolean rv = false;
        for (E item: c) {
            rv |= add(item);
        }
        return rv;
    }

    @Override
    public boolean contains(Object e) {
        checkIndex();
        return indexMap.containsKey(e);
    }

    @Override
    public int indexOf(Object e) {
        checkIndex();
        if (e == null) return -1;
        Integer i = indexMap.get(e);
        return (i == null) ? -1 : i;
    }

    @Override
    public int lastIndexOf(Object e) {
        return indexOf(e);
    }

    @Override @SuppressWarnings("unchecked")
    public Object clone() {
        IndexedArraySet clone = (IndexedArraySet) super.clone();
        clone.indexMap = (HashMap) indexMap.clone();
        return clone;
    }

    @Override
    public void add(int idx, E e) {
        invalidateIndex();
        if(indexMap.putIfAbsent(requireNonNull(e), -2) != null) return;
        super.add(idx, e);
        acceptReindex();
    }

    @Override
    public boolean remove(Object e) {
        boolean rv;
        invalidateIndex();
        try { rv = super.remove(e); }
        finally { acceptReindex(); }
        return rv;
    }

    @Override
    public void clear() {
        super.clear();
        indexMap.clear();
    }

    @Override
    public boolean addAll(int idx, Collection<? extends E> c) {
        invalidateIndex();
        List<E> tail = new ArrayList<>(this).subList(idx, size());
        removeRange(idx, size());
        reindex();
        boolean rv = addAll(c);
        addAll(tail);
        acceptIndex();
        return rv;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean rv;
        invalidateIndex();
        try { rv = super.removeAll(c); }
        finally { acceptReindex(); }
        return rv;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean rv;
        invalidateIndex();
        try { rv = super.retainAll(c); }
        finally { acceptReindex(); }
        return rv;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        boolean rv;
        invalidateIndex();
        try { rv = super.removeIf(filter); }
        finally { acceptReindex(); }
        return rv;
    }

    @Override
    public void replaceAll(final UnaryOperator<E> operator) {
        invalidateIndex();
        List<E> copy = new ArrayList<>(this);
        clear();
        try {
            copy.replaceAll(operator);
        } finally {
            addAll(copy);
            acceptIndex();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        invalidateIndex();
        try { super.sort(c); }
        finally { acceptReindex(); }
    }
}
