package org.transmartproject.batch.concept

import groovy.transform.CompileStatic

/**
 * A concept fragment that's not so fragment. Represents a full path.
 */
@CompileStatic
class ConceptPath extends ConceptFragment {
    ConceptPath(List<String> parts) {
        super(parts)
        if (parts.empty) {
            throw new IllegalArgumentException('Full paths cannot be empty')
        }
    }

    ConceptPath(String path) {
        super(path)
        if (parts.empty) {
            throw new IllegalArgumentException('Full paths cannot be empty')
        }
        if (this.path[0] != DELIMITER) {
            throw new IllegalArgumentException("Path should start with ${DELIMITER}")
        }
    }

    ConceptPath(ConceptFragment fragment) {
        super(fragment.path, fragment.parts)
        if (parts.empty) {
            throw new IllegalArgumentException('Full paths cannot be empty')
        }
    }

    /* override to return subtype */
    ConceptPath getParent() {
        super.parent ? new ConceptPath(super.parent) : null
    }

    /* override to return subtype */
    ConceptPath plus(ConceptFragment otherFragment) {
        new ConceptPath(this.parts + otherFragment.parts as List)
    }

    /* override to return subtype */
    ConceptPath plus(String otherPathFragment) {
        new ConceptPath(super.plus(otherPathFragment))
    }

    ConceptPath commonParent(ConceptPath otherPath) {
        def myParents = this.allParents
        def itsParents = otherPath.allParents
        def commonParents = myParents.intersect(itsParents)

        if (!commonParents) {
            null
        } else {
            commonParents.max { it.parts.size() }
        }
    }

    Set<ConceptPath> getAllParents() {
        def res = [] as Set
        for (ConceptPath p = this; p != null; p = p.parent) {
            res << p
        }
        res
    }
}
