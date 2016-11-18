package org.transmartproject.search.indexing

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import groovy.transform.CompileStatic
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.StringDescription
import org.hamcrest.TypeSafeMatcher

import static org.hamcrest.Matchers.*

class FacetsDocumentMatcher extends TypeSafeMatcher<FacetsDocument> {
    Matcher<String> idStringMatcher
    Multimap<Matcher<FacetsFieldImpl>, Matcher> fieldValuesMatchers = HashMultimap.create()
    Matcher<File> fileMatcher

    static FacetsDocumentMatcher documentWithFields(id, ...fields) {
        def ret = new FacetsDocumentMatcher()
        if (id instanceof Matcher) {
            ret.idStringMatcher = (Matcher) id
        }
        fields.each { Object field ->
            Matcher keyMatcher,
                    valueMatcher = any(Object)

            if (field instanceof String) {
                keyMatcher = hasProperty('fieldName', is(field))
            } else if (field instanceof Matcher) {
                keyMatcher = (Matcher) field
            } else if (field instanceof List && field.size() == 2) {
                List f = field as List
                if (f[0] instanceof String) {
                    keyMatcher = hasProperty('fieldName', is(f[0]))
                } else if (f[0] instanceof Matcher) {
                    keyMatcher = (Matcher) f[0]
                } else {
                    throw new IllegalArgumentException("Expectes String or Matcher, got ${f[0]}")
                }

                if (f[1] instanceof String || f[1] instanceof Number) {
                    valueMatcher = is(f[1])
                } else if (f[1] instanceof Matcher) {
                    valueMatcher = (Matcher) f[1]
                } else {
                    throw new IllegalArgumentException("Expectes String or Matcher, got ${f[0]}")
                }
            } else {
                throw new IllegalArgumentException("Expectes String, Matcher or size 2 list, got $field")
            }

            ret.fieldValuesMatchers.put(keyMatcher, valueMatcher)
        }

        ret
    }


    @Override
    protected boolean matchesSafely(FacetsDocument item) {
        def description = new StringDescription()
        describeMismatchSafely(item, description)
        description.toString() == ''
    }

    @Override
    protected void describeMismatchSafely(FacetsDocument item, Description mismatchDescription) {
        if (idStringMatcher) {
            if (!idStringMatcher.matches(item.facetsDocId.toString())) {
                mismatchDescription.appendText('has doc id: ')
                        .appendValue(item.facetsDocId.toString())
            }
        }
        if (fileMatcher) {
            if (!fileMatcher.matches(item.file)) {
                mismatchDescription.appendText('has file: ')
                        .appendValue(item.file)
            }
        }

        fieldValuesMatchers.entries().each { Map.Entry<Matcher, Matcher> entry ->
            def fieldMatcher = entry.key
            def valueMatcher = entry.value

            def matchingEntries = item.fieldValues.entries().findAll { Map.Entry entry2 ->
                fieldMatcher.matches(entry2.key)
            }

            if (!matchingEntries) {
                mismatchDescription.appendText('no field matching ')
                        .appendDescriptionOf(fieldMatcher)
                        .appendText('. Possible fields are: ')
                        .appendValue(item.fieldValues.keySet())
                        .appendText('\n')
                return
            }

            def allValues = matchingEntries*.value
            def matchingValues = allValues.any {
                valueMatcher.matches(it)
            }

            if (!matchingValues) {
                mismatchDescription.appendText('from the values ')
                        .appendValueList('{', ', ', '}', allValues)
                        .appendText(' nothing matching ')
                        .appendDescriptionOf(valueMatcher)
                        .appendText(' for field matching ')
                        .appendDescriptionOf(fieldMatcher)
                        .appendText('\n')
            }
        }
    }

    @Override
    void describeTo(Description description) {
        if (idStringMatcher) {
            description
                    .appendText('has string id matching ')
                    .appendDescriptionOf(idStringMatcher)
        }
        if (fileMatcher) {
            description
                    .appendText('has file matching ')
                    .appendDescriptionOf(fileMatcher)
        }
        if (!fieldValuesMatchers.empty) {
            fieldValuesMatchers.entries().each { Map.Entry<Matcher<FacetsFieldImpl>, Matcher> e ->
                description
                        .appendText('has field matching ')
                        .appendDescriptionOf(e.key)
                        .appendText(', with value ')
                        .appendDescriptionOf(e.value)
                        .appendText('\n')
            }
        }
    }
}
