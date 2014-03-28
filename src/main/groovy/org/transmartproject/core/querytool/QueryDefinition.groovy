package org.transmartproject.core.querytool

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.users.ProtectedResource

/**
 * A query definition is a set of panels. The data it represents is the
 * intersection of all the panels.
 */
@EqualsAndHashCode
final class QueryDefinition implements ProtectedResource {

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
                "${new Date().format('E MMM d yyyy HH:mm:ss \'GMT\'Z')}"
        this.panels = panels.asImmutable()
    }

    QueryDefinition(String name, List<Panel> panels) {
        this.name = name
        this.panels = panels.asImmutable()
    }
}
