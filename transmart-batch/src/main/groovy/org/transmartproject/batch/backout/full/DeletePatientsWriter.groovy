package org.transmartproject.batch.backout.full

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.patient.Patient

/**
 * Deletes the patients that come in the chunk.
 */
@BackoutComponent
@Slf4j
class DeletePatientsWriter implements ItemWriter<Patient> {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    void write(List<? extends Patient> items) throws Exception {
        int[] affected = jdbcTemplate.batchUpdate("""
                DELETE FROM $Tables.PATIENT_DIMENSION
                WHERE patient_num = :id""",
                items.collect { [id: it.code] } as Map[])
        DatabaseUtil.checkUpdateCountsPermissive affected,
                "Delete from $Tables.PATIENT_DIMENSION", items

        affected = jdbcTemplate.batchUpdate("""
                DELETE FROM $Tables.PATIENT_TRIAL
                WHERE patient_num = :id""",
                items.collect { [id: it.code] } as Map[])
        DatabaseUtil.checkUpdateCountsPermissive affected,
                "Delete from $Tables.PATIENT_DIMENSION", items, true
    }
}
