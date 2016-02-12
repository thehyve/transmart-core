package org.transmartproject.batch.highdim.rnaseq.data

import org.transmartproject.batch.highdim.datastd.TripleStandardDataValue

/**
 * Extends {@link TripleStandardDataValue} to add read count.
 * {@link TripleStandardDataValue#value} is normalized read count.
 */
class RnaSeqDataValue extends TripleStandardDataValue {
    Double readCount
}
