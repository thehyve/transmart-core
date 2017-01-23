package org.transmartproject.batch.i2b2.dimensions

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY

/**
 * Searches the database for existing patients and writes them to the
 * {@link DimensionsStore}.
 */
@Component
@JobScope
@Slf4j
class RegisterExistingPatientsWriter implements ItemWriter<String> {

    public static final String PROJECT_ID_COLUMN = 'project_id'
    public static final String PATIENT_EXTERNAL_ID_COLUMN = 'patient_ide'
    public static final String PATIENT_EXTERNAL_ID_SOURCE_COLUMN = 'patient_ide_source'
    public static final String PATIENT_INTERNAL_ID_COLUMN = 'patient_num'

    @Value("#{jobParameters['PATIENT_IDE_SOURCE']}")
    private String patientIdeSource

    @Value("#{jobParameters['PROJECT_ID']}")
    private String projectId

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Autowired
    private DimensionsStore dimensionsStore

    @Value('#{tables.patientMapping}')
    private String patientMappingTable

    @Override
    void write(List<? extends String> items) throws Exception {
        Object[] params = new Object[items.size() + 2]

        items.eachWithIndex { String item, int index ->
            params[index] = item
        }
        params[-2] = patientIdeSource
        params[-1] = projectId

        List<Map<String, Object>> result = jdbcTemplate.queryForList """
                SELECT
                    $PATIENT_EXTERNAL_ID_COLUMN,
                    $PATIENT_INTERNAL_ID_COLUMN
                FROM $patientMappingTable
                WHERE
                    $PATIENT_EXTERNAL_ID_COLUMN IN (${items.collect { '?' }.join ', '})
                    AND $PATIENT_EXTERNAL_ID_SOURCE_COLUMN = ?
                    AND $PROJECT_ID_COLUMN = ?""",
                params

        log.debug("Patient mappings: from ${items.size()} items, " +
                "found ${result.size()}")

        Set<String> notSeen = Sets.newTreeSet(items)
        result.each { Map<String, Object> map ->
            log.trace("Patients: seen $map")
            dimensionsStore.syncWithDatabaseEntry(
                    PATIENT_DIMENSION_KEY,
                    map[PATIENT_EXTERNAL_ID_COLUMN],
                    map[PATIENT_INTERNAL_ID_COLUMN] as String,
                    (String) null)

            notSeen.remove(map[PATIENT_EXTERNAL_ID_COLUMN])
        }

        // generally not needed (not seen is the default state), but
        // useful if restarting the step after changing the database
        log.trace("Patients: not seen: $notSeen")
        notSeen.each {
            dimensionsStore.markAsNotInDatabase(PATIENT_DIMENSION_KEY, it)
        }
    }
}
