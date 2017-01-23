package org.transmartproject.batch.i2b2.dimensions

import com.google.common.collect.Sets
import groovy.util.logging.Slf4j
import org.springframework.batch.core.UnexpectedJobExecutionException
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Searches the database for existing visits and writes them to the
 * {@link DimensionsStore}.
 */
@Component
@JobScope
@Slf4j
class RegisterExistingVisitsWriter implements ItemWriter<String> {

    public static final String PROJECT_ID_COLUMN = 'project_id'
    public static final String VISIT_EXTERNAL_ID_COLUMN = 'encounter_ide'
    public static final String VISIT_EXTERNAL_ID_SOURCE_COLUMN = 'encounter_ide_source'
    public static final String VISIT_INTERNAL_ID_COLUMN = 'encounter_num'
    public static final String PATIENT_EXTERNAL_ID_COLUMN = 'patient_ide'
    public static final String PATIENT_EXTERNAL_ID_SOURCE_COLUMN = 'patient_ide_source'

    @Value("#{jobParameters['VISIT_IDE_SOURCE']}")
    private String visitIdeSource

    @Value("#{jobParameters['PATIENT_IDE_SOURCE']}")
    private String patientIdeSource

    @Value("#{jobParameters['PROJECT_ID']}")
    private String projectId

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Autowired
    private DimensionsStore dimensionsStore

    @Value('#{tables.encounterMapping}')
    private String encounterMappingTable

    @Override
    void write(List<? extends String> items) throws Exception {
        Object[] params = new Object[items.size() + 2]

        items.eachWithIndex { String item, int index ->
            params[index] = item
        }
        params[-2] = visitIdeSource
        params[-1] = projectId

        List<Map<String, Object>> result = jdbcTemplate.queryForList """
                SELECT
                    $VISIT_EXTERNAL_ID_COLUMN,
                    $VISIT_INTERNAL_ID_COLUMN,
                    $PATIENT_EXTERNAL_ID_COLUMN,
                    $PATIENT_EXTERNAL_ID_SOURCE_COLUMN
                FROM ${encounterMappingTable}
                WHERE
                    $VISIT_EXTERNAL_ID_COLUMN IN (${items.collect { '?' }.join ', '})
                    AND $VISIT_EXTERNAL_ID_SOURCE_COLUMN = ?
                    AND $PROJECT_ID_COLUMN = ?""",
                params

        log.debug("Visit mappings: from ${items.size()} items, " +
                "found ${result.size()}")

        Set<String> notSeen = Sets.newTreeSet(items)
        result.each { Map<String, Object> map ->
            log.trace("Visits: seen $map")
            if (map[PATIENT_EXTERNAL_ID_SOURCE_COLUMN] != patientIdeSource) {
                throw new UnexpectedJobExecutionException(
                        "Visit ${map[VISIT_EXTERNAL_ID_COLUMN]} for source " +
                                "$visitIdeSource is associated with a " +
                                "patient with source " +
                                "${map[PATIENT_EXTERNAL_ID_SOURCE_COLUMN]}, " +
                                "but we only support patients with source " +
                                "$patientIdeSource in this run")
            }

            dimensionsStore.syncWithDatabaseEntry(
                    VISITS_DIMENSION_KEY,
                    map[VISIT_EXTERNAL_ID_COLUMN],
                    map[VISIT_EXTERNAL_ID_SOURCE_COLUMN],
                    map[PATIENT_EXTERNAL_ID_COLUMN])

            notSeen.remove(map[VISIT_EXTERNAL_ID_COLUMN])
        }

        // generally not needed, but (not seen is the default state), but
        // useful if restarting the step after changing the database
        log.trace("Visits: not seen: $notSeen")
        notSeen.each {
            dimensionsStore.markAsNotInDatabase(VISITS_DIMENSION_KEY, it)
        }
    }
}
