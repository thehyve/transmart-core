package org.transmartproject.batch.concept

import com.google.common.base.Splitter
import com.google.common.collect.Lists
import groovy.transform.EqualsAndHashCode

/**
 * Represents a (possibly empty) list of path components in a concept path.
 */
@EqualsAndHashCode(includes = 'path')
class ConceptFragment implements Comparable<ConceptFragment> {
    final String path
    final List<String> parts

    protected ConceptFragment(String path, List<String> parts) {
        this.path = path
        this.parts = parts
    }

    ConceptFragment(List<String> parts) {
        if (parts.any { it.contains '\\' }) {
            throw new IllegalArgumentException('None of the parts can include' +
                    ' the \'\\\' character; got ' + parts)
        }
        if ('' in parts) {
            throw new IllegalArgumentException(
                    "Path cannot have empty parts (got '$parts')")
        }

        this.parts = parts
        this.path = parts.empty ? '\\' : "\\${parts.join('\\')}\\"
    }

    ConceptFragment(String path) {
        if (path.size() == 0 || path[0] != '\\') {
            throw new IllegalArgumentException('Path should start with \\')
        }
        if (path.size() == 0) {
            throw new IllegalArgumentException('Path is empty')
        }
        if (path[-1] != '\\') {
            path += '\\'
        }

        this.path = path

        // trim the first leading and trailing backslash for split
        path = path.replaceFirst('^\\\\', '').replaceFirst('\\\\$', '')
        if (path != '') { // originally \
            parts = Lists.newArrayList(Splitter.on('\\').split(path))
        } else {
            parts = []
        }

        if ('' in parts) {
            throw new IllegalArgumentException(
                    "Path cannot have empty parts (got '$path')")
        }
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
        new ConceptFragment('\\' + parts[0..-2].join('\\') + '\\')
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
        def stringFragment = otherPathFragment
        if (!otherPathFragment.startsWith('\\')) {
            stringFragment = '\\' + stringFragment
        }
        this + new ConceptFragment(stringFragment)
    }
}
