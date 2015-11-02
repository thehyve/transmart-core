package org.transmartproject.batch.db

import org.transmartproject.batch.beans.Postgresql

/**
 * Implementation of {@link SequenceReserver} for PostgreSQL.
 */
@Postgresql
class PostgresSequenceReserver extends SequenceReserver {

    private static final SQL = 'SELECT nextval(:sequence) FROM generate_series(1, :blockSize)'

    @Override
    List<Long> getValuesFromDatabase(String sequence, long blockSize) {
        template.queryForList(SQL, [sequence: sequence, blockSize: blockSize], Long)
    }
}
