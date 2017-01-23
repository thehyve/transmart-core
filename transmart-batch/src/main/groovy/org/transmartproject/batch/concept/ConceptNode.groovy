package org.transmartproject.batch.concept

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents a concept.
 */
@SuppressWarnings('DuplicateListLiteral') // bug in codenarc
@ToString(includes = ['code', 'path'])
@EqualsAndHashCode(includes = ['code', 'path'])
@CompileStatic
class ConceptNode {
    int level
    ConceptPath path
    String name
    String code

    ConceptType type = ConceptType.UNKNOWN

    ConceptNode() {
        // empty constructor to allow specifying properties
    }

    ConceptNode(ConceptPath path) {
        this.path = path
        this.level = path.length - 1 /* path with 1 component has level 0 */
        this.name = path[-1]
    }

    ConceptNode(String path) {
        this(new ConceptPath(path))
    }
}
