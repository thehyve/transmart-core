package org.transmartproject.batch.highdim.mirna.platform

import groovy.transform.Canonical

/**
 * Represents a line on the miRNA annotations file
 */
@Canonical
class MirnaAnnotationRow {
    String idRef
    String mirnaId
}
