package org.transmartproject.core.querytool

import groovy.transform.EqualsAndHashCode

import java.text.SimpleDateFormat

/**
 * A query definition is a set of panels. The data it represents is the
 * intersection of all the panels.
 */
@EqualsAndHashCode
final class QueryDefinition {

    /**
     * The query name. This is a user friendly name and has no bearing on the
     * program's operation.
     */
    final String name

    /**
     * The panels used for this query definition. The data encompassed by the
     * panels will be intersected.
     */
    final List<Panel> panels

    QueryDefinition(List<Panel> panels) {
        this.name = "tranSMART's Query at " +
                "${new SimpleDateFormat('E MMM d yyyy HH:mm:ss \'GMT\'Z').format(new Date())}"
        this.panels = panels.asImmutable()
    }

    QueryDefinition(String name, List<Panel> panels) {
        this.name = name
        this.panels = panels.asImmutable()
    }
}
