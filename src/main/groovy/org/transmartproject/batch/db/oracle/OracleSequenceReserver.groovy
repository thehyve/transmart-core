package org.transmartproject.batch.db.oracle

import org.transmartproject.batch.beans.Oracle
import org.transmartproject.batch.db.SequenceReserver

/**
 * Implementation of {@link SequenceReserver} for PostgreSQL.
 */
@Oracle
class OracleSequenceReserver extends SequenceReserver {

    private static final SQL_TEMPLATE = 'SELECT %s.nextval FROM DUAL CONNECT BY LEVEL <= :blockSize'

    @Override
    List<Long> getValuesFromDatabase(String sequence, long blockSize) {
        template.queryForList(String.format(SQL_TEMPLATE, sequence), [sequence: sequence, blockSize: blockSize], Long)
        template.queryForList(String.format(SQL_TEMPLATE, sequence),
                [blockSize: blockSize], Long)
    }
}
