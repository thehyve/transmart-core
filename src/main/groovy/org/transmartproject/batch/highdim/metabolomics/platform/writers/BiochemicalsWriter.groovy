package org.transmartproject.batch.highdim.metabolomics.platform.writers

import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.metabolomics.platform.model.Biochemical

import java.sql.PreparedStatement
import java.sql.Types

/**
 * Insert the metabolites.
 */
@Component
class BiochemicalsWriter extends AbstractMetabolomicsWriter<Biochemical> {

    final String sql = """
        INSERT INTO ${Tables.METAB_ANNOTATION}
        (id, gpl_id, biochemical_name, hmdb_id)
        VALUES (?, ?, ?, ?)
    """

    final Closure preparedStatementSetter = { Biochemical item, PreparedStatement ps ->
        ps.setLong(1, item.id)
        ps.setString(2, platform.id)
        ps.setString(3, item.name)
        if (item.hmdbId) {
            ps.setString(4, item.hmdbId)
        } else {
            ps.setNull(4, Types.VARCHAR)
        }
    }
}
