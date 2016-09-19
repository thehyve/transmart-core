package org.transmartproject.batch.highdim.biomarker

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Retrieves biomarker information from the database.
 */
@Component
@Slf4j
class BioMarkerRepository {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    /*PrimaryExternalIdMappings primaryExternalIdMappingsFor(String biomarkerType, String organism) {
        // right now we only care about genes, which have long peids
        def mappings = new PrimaryExternalIdMappings(
                biomarkerType: biomarkerType,
                organism: organism,
                primaryExternalIdType: Long)

        // some times the organism is uppercased...
        jdbcTemplate.query('''
            SELECT bio_marker_name, primary_external_id
            FROM biomart.bio_marker
            WHERE (organism = :organism OR organism = :organism_upper)
                AND bio_marker_type = :bio_marker_type''', [
                        organism: organism,
                        organism_upper: organism.toUpperCase(),
                        biomarkerType: biomarkerType,
                ],
                { ResultSet rs ->
                    String bioMarkerName = rs.getString(1)
                    String peidString = rs.getString(2)


                    if (!bioMarkerName) {
                        log.warn
                    }
                } as RowCallbackHandler)
    }*/
}
