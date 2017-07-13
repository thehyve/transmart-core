package org.transmartproject.batch.concept

import com.google.common.base.Splitter
import com.google.common.collect.Lists
import groovy.transform.EqualsAndHashCode

/**
 * Represents a (possibly empty) list of path components in a concept path.
 */
@EqualsAndHashCode(includes = 'path')
class ConceptFragment implements Comparable<ConceptFragment> {
    public static final String DELIMITER = '\\'
    public static final Splitter SPLITTER = Splitter.on(DELIMITER)
    final String path
    final List<String> parts

    protected ConceptFragment(String path, List<String> parts) {
        this.path = path
        this.parts = parts
    }

    ConceptFragment(List<String> parts) {
        if (parts.any { it.contains DELIMITER }) {
            throw new IllegalArgumentException('None of the parts can include' +
                    ' the \'\\\' character; got ' + parts)
        }
        if ('' in parts) {
            throw new IllegalArgumentException(
                    "Path cannot have empty parts (got '$parts')")
        }

        this.parts = parts
        this.path = toPath(parts)
    }

    private String toPath(List<String> parts) {
        if (parts.empty) {
            DELIMITER
        } else {
            DELIMITER + parts.join(DELIMITER) + DELIMITER
        }
    }

    ConceptFragment(String path) {
        if (path.empty) {
            throw new IllegalArgumentException('Path is empty')
        }
        if (path[-1] != DELIMITER) {
            path += DELIMITER
        }

        this.path = path

        // trim the first leading and trailing backslash for split
        path = path.replaceFirst('^\\\\', '').replaceFirst('\\\\$', '')
        if (path != '') { // originally \
            parts = Lists.newArrayList(SPLITTER.split(path))
        } else {
            parts = []
        }

        if ('' in parts) {
            throw new IllegalArgumentException(
                    "Path cannot have empty parts (got '$path')")
        }
    }
    static ConceptFragment decode(String encodedConceptPath) {
        // We need to double the escape chars in DELIMITER since it's interpreted again by replaceAll
        String escapedDelimiter = DELIMITER.replace('\\','\\\\')

        // Use negative lookbehind to allow + and _ to be escaped by preceding it with a backslash
        String conceptPath = encodedConceptPath
                // replace underscores not preceded by backslash with space
                .replaceAll('(?<!\\\\)_',' ')
                // replace +'es not preceded by backslash with the delimiter
                .replaceAll('(?<!\\\\)\\+', escapedDelimiter)
                // replace \+ with +
                .replace('\\+','+')
                // replace \_ with _
                .replace('\\_','_')

        new ConceptFragment(conceptPath)
    }

    def getAt(int index) {
        def calculatedIndex
        if (index < 0) {
            calculatedIndex = parts.size() + index
        } else {
            calculatedIndex = index
        }

        calculatedIndex < parts.size() ? parts[calculatedIndex] : null
    }

    def getAt(Range range) {
        parts[range]
    }

    int getLength() {
        parts.size()
    }

    ConceptFragment getParent() {
        if (length == 1) {
            return null
        }
        new ConceptFragment(DELIMITER + parts[0..-2].join(DELIMITER) + DELIMITER)
    }

    boolean isSiblingOf(ConceptFragment otherFragment) {
        otherFragment.length == this.length &&
                this.length == 1 ||
                otherFragment.parts[1..-1] == this.parts[1..-1]
    }

    boolean isPrefixOf(ConceptFragment otherFragment) {
        otherFragment.path.startsWith(this.path)
    }

    ConceptFragment removePrefix(ConceptFragment prefix) {
        if (!prefix.isPrefixOf(this)) {
            throw new IllegalArgumentException("$prefix is not a prefix of $this")
        }

        new ConceptFragment(this.parts.subList(
                prefix.parts.size(), this.parts.size()))
    }

    @Override
    String toString() {
        path
    }

    @Override
    int compareTo(ConceptFragment other) {
        path <=> other.path
    }

    ConceptFragment plus(ConceptFragment otherFragment) {
        new ConceptFragment(this.parts + otherFragment.parts)
    }

    ConceptFragment plus(String otherPathFragment) {
        this + new ConceptFragment(otherPathFragment)
    }
}
