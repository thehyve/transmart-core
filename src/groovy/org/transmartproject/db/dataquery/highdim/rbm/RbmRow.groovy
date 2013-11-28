package org.transmartproject.db.dataquery.highdim.rbm

import groovy.transform.ToString
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

@ToString(excludes = [ 'assayIndexMap' ])
class RbmRow extends AbstractDataRow {

    Long rbmAnnotationId

    String geneSymbol

    String uniprotId

    @Override
    String getLabel() {
        uniprotId
    }
}
