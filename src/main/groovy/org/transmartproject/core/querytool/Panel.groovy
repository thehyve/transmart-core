package org.transmartproject.core.querytool

/**
 * A panel represents a definition used to query the data marts. A panel has
 * several items (concept keys); the data it represents is the union of the
 * several items.
 *
 * Note that this panel definition is significantly more limited than i2b2's.
 * For instance, no constraints can be added and only dimension path keys are
 * supported as item_keys. Items cannot include patient sets,
 * encounter sets or other queries.
 */
class Panel {

    /**
     * Whether to invert this panel.
     */
    final boolean invert

    /**
     * The concept keys to be OR-ed together.
     */
    final List<String> conceptKeys

    /**
     * The panel constructor.
     *
     * @param conceptKeys the concept keys to union together
     * @param invert whether to take the set's complement instead
     */
    Panel(List<String> conceptKeys, boolean invert = false) {
        this.conceptKeys = conceptKeys.asImmutable()
        this.invert = invert
    }

}
