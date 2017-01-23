package org.transmartproject.batch.highdim.metabolomics.platform.writers

import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.metabolomics.platform.model.Biochemical

import java.sql.PreparedStatement

/**
 * Insert the associations between the metabolites and the sub pathways.
 */
@Component
class BiochemicalSubPathwayAssociationWriter extends AbstractMetabolomicsWriter<Biochemical> {

    final String sql = """
        INSERT INTO ${Tables.METAB_ANNOT_SUB}
        (metabolite_id, sub_pathway_id)
        VALUES (?, ?)
    """

    final Closure preparedStatementSetter = { Biochemical item, PreparedStatement ps ->
        ps.setLong(1, item.id)
        ps.setLong(2, item.subPathway.id)
    }
}
