package org.transmartproject.search.indexing

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import groovy.transform.ToString
import org.transmartproject.search.browse.BioDataValuesView

@ToString
class FacetsDocument implements Comparable<FacetsDocument> {
    FacetsDocId facetsDocId
    Multimap<FacetsFieldImpl, Object> fieldValues
    File file

    @Override
    int compareTo(FacetsDocument o) {
        facetsDocId.toString() <=> o?.facetsDocId?.toString()
    }

    static class FieldValuesBuilder extends ImmutableMultimap.Builder<FacetsFieldImpl, Object> {
        @Override
        FieldValuesBuilder put(FacetsFieldImpl key, Object value) {
            if (value != null) {
                return super.put(key, value)
            }
            this
        }

        FieldValuesBuilder putAt(FacetsFieldImpl key, List<Object> list) {
            list.each {
                putAt key, it
            }
            this
        }

        FieldValuesBuilder putAt(FacetsFieldImpl key, String[] array) {
            putAt(key, array as List)
        }

        FieldValuesBuilder putAt(FacetsFieldImpl key, BioDataValuesView value) {
            put key, value.name
            put FacetsIndexingService.FIELD_TEXT, value.description
            this
        }

        FieldValuesBuilder putAt(FacetsFieldImpl key, Object value) {
            put key, value
            this
        }
    }

    static newFieldValuesBuilder() {
        new FieldValuesBuilder()
    }

    static FacetsDocument merge(Collection<FacetsDocument> docs) {
        if (docs.size() == 1) {
            return docs.first()
        }

        def uniqueIds = (docs*.facetsDocId as Set)
        def uniqueFiles = (docs*.file as Set)
        if (uniqueIds.size() != 1) {
            throw new IllegalArgumentException("Different ids: $uniqueIds")
        }
        if (uniqueFiles.size() != 1) {
            throw new IllegalArgumentException("Different files: $uniqueFiles")
        }

        def values = HashMultimap.create() // deletes duplicates on value sets
        docs.each {
            values.putAll(it.fieldValues)
        }

        new FacetsDocument(
                facetsDocId: uniqueIds.first(),
                fieldValues: values,
                file: uniqueFiles.first())
    }
}
