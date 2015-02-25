package org.transmartproject.batch.highdim.metabolomics.platform.writers

import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.metabolomics.platform.model.SuperPathway

import java.sql.PreparedStatement

/**
 * Insert the super pathways.
 */
@Component
class SuperPathwaysWriter extends AbstractMetabolomicsWriter<SuperPathway> {

    final String sql = """
        INSERT INTO ${Tables.METAB_SUPER_PATH}
        (id, gpl_id, super_pathway_name)
        VALUES (?, ?, ?)
    """

    final Closure preparedStatementSetter = { SuperPathway item, PreparedStatement ps ->
        ps.with {
            setLong   1, item.id
            setString 2, platform.id
            setString 3, item.name
        }
    }
}
