package org.transmartproject.batch.highdim.metabolomics.platform.writers

import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.metabolomics.platform.model.SubPathway
import org.transmartproject.batch.highdim.metabolomics.platform.model.SuperPathway

import java.sql.PreparedStatement
import java.sql.Types

/**
 * Insert the sub pathways.
 */
@Component
class SubPathwaysWriter extends AbstractMetabolomicsWriter<SuperPathway> {

    final String sql = """
        INSERT INTO ${Tables.METAB_SUB_PATH}
        (id, gpl_id, sub_pathway_name, super_pathway_id)
        VALUES (?, ?, ?, ?)
    """

    final Closure preparedStatementSetter = { SubPathway item, PreparedStatement ps ->
        ps.with {
            setLong(1, item.id)
            setString(2, platform.id)
            setString(3, item.name)
            if (item.superPathway) {
                setLong(4, item.superPathway.id)
            } else {
                setNull(4, Types.BIGINT)
            }
        }
    }
}
