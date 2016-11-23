package org.transmartproject.db.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.Comparator;
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

    private HashMap<E, Integer> indexMap = new HashMap<>();

    private void reindex() {
        indexMap.clear();
        int idx = 0;
        for (E item: this) {
            addToIndex(item, idx++);
        }
    }

    private E addToIndex(E e, int idx) {
        indexMap.putIfAbsent(requireNonNull(e), idx);
        return e;
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

    @Override @SuppressWarnings("unchecked")
    public Object clone() {
        IndexedArraySet clone = (IndexedArraySet) super.clone();
        clone.indexMap = (HashMap) indexMap.clone();
        return clone;
    }

    @Override
    public void add(int idx, E e) {
        if(indexMap.putIfAbsent(requireNonNull(e), -1) != null) return;
        super.add(idx, e);
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
            int duplicates = 0;
            for (int i = 0; i < size(); i++) {
                E newval = requireNonNull(operator.apply(this.get(i)));
                if(indexMap.putIfAbsent(newval, i-duplicates) == null) {
                    super.set(i-duplicates, newval);
                } else {
                    duplicates++;
                }
            }
            removeRange(size()-duplicates, size());
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
