package org.transmartproject.batch.concept.oracle

import org.transmartproject.batch.beans.Oracle
import org.transmartproject.batch.concept.InsertConceptCountsTasklet

/**
 * Insert concept counts, Oracle version.
 *
 * Not implemented.
 */
@Oracle
class OracleInsertConceptCountsTasklet extends InsertConceptCountsTasklet {
    @Override
    String getSql() {
        throw new UnsupportedOperationException('Not yet implemented')
    }
}
