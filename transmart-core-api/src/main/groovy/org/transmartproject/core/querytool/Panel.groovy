package org.transmartproject.core.querytool

import groovy.transform.Immutable

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
@Immutable
class Panel {

    /**
     * Whether to invert this panel.
     */
    boolean invert

    /**
     * The items to be OR-ed together.
     */
    List<Item> items
}
