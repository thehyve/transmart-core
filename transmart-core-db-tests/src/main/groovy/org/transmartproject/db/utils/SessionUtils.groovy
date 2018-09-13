package org.transmartproject.db.utils

import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.hibernate.jdbc.Work
import org.hibernate.type.StandardBasicTypes

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Database utils.
 * Assumes in some places that hibernate and H2 are used.
 * @see org.transmartproject.db.util.HibernateUtils
 */
@Slf4j
class SessionUtils {

    static List<String> getAllTables(Session session) {
        final List<String> resourceFullyQalifiedNames = []

        session.doWork(new Work() {
            @Override
            void execute(Connection connection) throws SQLException {
                resourceFullyQalifiedNames.addAll(getAllTables(connection))
            }
        })

        return resourceFullyQalifiedNames
    }

    static List<String> getAllTables(Connection providedConnection) {
        final List<String> resourceFullyQalifiedNames = []

        ResultSet tableMetadataRs = providedConnection.getMetaData().getTables(null, null, null, ['TABLE'] as String[])
        while (tableMetadataRs.next()) {
            String schema = tableMetadataRs.getString('TABLE_SCHEM').toLowerCase()
            String table = tableMetadataRs.getString('TABLE_NAME').toLowerCase()
            resourceFullyQalifiedNames << [schema, table].join('.')
        }

        return resourceFullyQalifiedNames
    }

    /**
     * Clear data globally, visible over all connections and transactions. You will need to restore any data manually
     * after calling this.
     */
    static void truncateTables(Session session, Iterable<String> tables) {
        log.info "Truncating all tables ..."
        session.createSQLQuery("set REFERENTIAL_INTEGRITY false").executeUpdate()

        for (String tableName : tables) {
            session.createSQLQuery("TRUNCATE TABLE ${tableName};").executeUpdate()
        }
        session.createSQLQuery("set REFERENTIAL_INTEGRITY true").executeUpdate()
        session.clear()
    }

    static long getNextId(Session session, String sequence) {
        Long nextId = session.createSQLQuery(
                "select ${sequence}.nextval as num")
                .addScalar("num", StandardBasicTypes.BIG_INTEGER)
                .uniqueResult() as Long
        log.debug("Next id from ${sequence} is ${nextId}")
        return nextId
    }

    static boolean resetIdSeq(Session session, String sequence, Integer startWith = 1) {
        log.info "Resetting sequence ${sequence} ..."
        boolean updated = session.createSQLQuery(
                "alter sequence ${sequence} restart with ${startWith}")
                .executeUpdate()
        log.debug("Reset ${sequence}: ${updated}")
        return updated
    }

}
