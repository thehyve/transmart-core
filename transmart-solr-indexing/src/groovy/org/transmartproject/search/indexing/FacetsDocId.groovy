package org.transmartproject.search.indexing

import groovy.transform.EqualsAndHashCode

/**
 * Created by glopes on 07-03-2016.
 */
@EqualsAndHashCode
class FacetsDocId {
    final String type
    final String id

    FacetsDocId(String fullId) {
        (type, id) = fullId.split(':', 2)
    }

    FacetsDocId(Map args) {
        assert args['type']
        assert args['id']
        type = args['type']
        id = args['id']
    }

    FacetsDocId(String type, String id) {
        this.type = type
        this.id = id
    }

    String getType() {
        type.toUpperCase(Locale.ENGLISH)
    }

    String toString() {
        "$type:$id"
    }
}
