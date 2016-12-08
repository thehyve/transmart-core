package org.transmartproject.search.indexing

import groovy.transform.Immutable

/**
 * Straightforward immutable implementation of {@link FacetsFieldDisplaySettings}.
 */
@Immutable
class SimpleFacetsFieldDisplaySettings implements FacetsFieldDisplaySettings {
    String displayName
    boolean hideFromListings
    int order

    @Override
    int compareTo(FacetsFieldDisplaySettings o) {
        this.order <=> o?.order ?: this.displayName <=> o?.displayName
    }
}
