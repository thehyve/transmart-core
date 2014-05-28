package org.transmartproject.db.concept

import com.google.common.base.Splitter
import com.google.common.collect.Lists
import groovy.transform.EqualsAndHashCode

/**
 * Represents an i2b2 concept full name.
 */
@EqualsAndHashCode
final class ConceptFullName {
    private final String fullPath
    final List<String> parts

    ConceptFullName(String path) {
        if (path.size() == 0 || path[0] != '\\')
            throw new IllegalArgumentException('Path should start with \\')
        if (path.size() < 2)
            throw new IllegalArgumentException('Path is too short')
        if (path[-1] != '\\')
            path += '\\'

        this.fullPath = path

        // trim the first leading and trailing backslash for split
        path = path.replaceFirst('^\\\\', '').replaceFirst('\\\\$', '')
        parts = Lists.newArrayList(Splitter.on('\\').split(path))

        if (parts.size() == 0 || '' in parts) {
            throw new IllegalArgumentException('Path cannot have empty parts')
        }
    }

    def getAt(int index) {
        if (index < 0)
            index = parts.size() + index

        index < parts.size() ? parts[index] : null
    }

    def getLength() {
        parts.size()
    }

    ConceptFullName getParent() {
        if (length == 1) {
            return null
        }
        new ConceptFullName('\\' + parts[0..-2].join('\\') + '\\')
    }

    boolean isSiblingOf(ConceptFullName otherFullName) {
        otherFullName.length == this.length &&
                this.length == 1 ||
                otherFullName.parts[1..-1] == this.parts[1..-1]
    }

    @Override
    public String toString() {
        fullPath
    }
}
