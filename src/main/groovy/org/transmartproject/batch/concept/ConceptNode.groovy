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
    Long i2b2RecordId

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

    /**
     * When a node is new then it means that it is not stored to a db yet.
     * NOTE: We use {@code i2b2RecordId} instead {@code code} as some concept nodes
     * (e.g. "\Private Studies\", "\Public Studies\") in a database might not have {@code code} filled in.
     * @return {@code true} if a node is new and {@code false} otherwise.
     */
    boolean isNew() {
        i2b2RecordId == null
    }
}
