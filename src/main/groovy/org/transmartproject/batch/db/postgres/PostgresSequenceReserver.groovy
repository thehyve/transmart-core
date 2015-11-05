package org.transmartproject.batch.db.postgres

import org.transmartproject.batch.beans.Postgresql
import org.transmartproject.batch.db.SequenceReserver

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
