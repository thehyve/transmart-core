package org.transmartproject.batch.highdim.platform

import groovy.transform.Canonical

/**
 * A de_gpl_info row.
 */
@Canonical
class Platform {
    String id /* platform column */
    String title
    String organism
    String markerType
    String genomeRelease
}
