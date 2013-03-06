package org.transmartproject.db.concept

import groovy.transform.EqualsAndHashCode

/**
 * Represents an i2b2 concept full name.
 */
@EqualsAndHashCode
final class ConceptFullName {
    private final String fullPath
    final List<String> parts;

    ConceptFullName(String fullPath) {
        if (fullPath.size() == 0 || fullPath[0] != '\\')
            throw new IllegalArgumentException('Path should start with \\')
        if (fullPath.size() < 2)
            throw new IllegalArgumentException('Path is too short')
        if (fullPath[-1] != '\\')
            fullPath += '\\'

        /* split cannot be used because it removes trailing empty elements */
        parts = new ArrayList()
        def matcher = (fullPath =~ '\\\\')
        for (int i = 1; matcher.find(i); ) {
            int location = matcher.start()
            parts.add(fullPath.substring(i, location))
            i = location + 1
        }

        if (parts.size() == 0 || parts.any({ it.empty })) {
            throw new IllegalArgumentException('Path cannot have empty parts')
        }

        this.fullPath = fullPath
    }

    def getAt(int index) {
        if (index < 0)
            index = parts.size() + index

        index < parts.size() ? parts[index] : null
    }

    def getLength() {
        parts.size()
    }

    @Override
    public String toString() {
        fullPath
    }
}
