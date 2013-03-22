package org.transmartproject.core.querytool

/**
 * A query definition is a set of panels. The data it represents is the
 * intersection of all the panels
 */
class QueryDefinition {

    final List<Panel> panels

    QueryDefinition(Panel[] panels) {
        this.panels = panels.toList().asImmutable()
    }

}
