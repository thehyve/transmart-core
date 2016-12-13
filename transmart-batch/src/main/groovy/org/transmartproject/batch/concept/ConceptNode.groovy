package org.transmartproject.batch.concept

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents a concept.
 */
@SuppressWarnings('DuplicateListLiteral') // bug in codenarc
@ToString(includes = ['code', 'path', 'conceptName', 'conceptPath', 'conceptCode'])
@EqualsAndHashCode(includes = ['code', 'path', 'conceptName', 'conceptPath', 'conceptCode'])
@CompileStatic
class ConceptNode {
    int level
    /**
     * Tree node path
     */
    ConceptPath path
    /**
     * Name of the tree node
     */
    String name
    /**
     * Concept name
     */
    String conceptName
    /**
     * Concept path
     */
    ConceptPath conceptPath
    /**
     * Concept code
     */
    String code
    /**
     * Concept URI
     */
    String uri

    /**
     * Flag if this node represents a shared ontology term (public access)
     */
    boolean ontologyNode = false

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
