package org.transmartproject.batch.biodata

import com.google.common.collect.ImmutableSet
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.SequenceReserver

/**
 * Retrieval and insertion of bio experiments.
 */
@Component
@Slf4j
@CompileStatic
class BioExperimentDAO {
    @Value(Tables.BIO_EXPERIMENT)
    private SimpleJdbcInsert bioExperimentInsert

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private SequenceReserver sequenceReserver

    private final Set<String> validKeys = ImmutableSet.of(
            'accession',
            'etl_id',
            'title',)

    Long findOrCreateBioExperiment(String studyId,
                                   Map<String, Object> extraData = [:]) {
        assert studyId != null
        def intersection = extraData.keySet().intersect(validKeys)
        if (intersection) {
            throw new IllegalArgumentException("Invalid keys: $intersection")
        }

        Map<String, Object> desiredValues = [
                accession: (Object) studyId,
                etl_id: "METADATA:$studyId" as String,
                title: 'Metadata not available',
        ]
        desiredValues.putAll(extraData)
        log.debug("Desired values for bio experiment are: $desiredValues")

        try {
            jdbcTemplate.update("LOCK TABLE $Tables.BIO_EXPERIMENT " +
                    "IN SHARE ROW EXCLUSIVE MODE", [:])

            def result = jdbcTemplate.queryForMap """
                    SELECT
                        bio_experiment_id AS id,
                        ${desiredValues.keySet().join(', ')}
                    FROM ${Tables.BIO_EXPERIMENT}
                    WHERE accession = :study""",
                    [study: studyId]

            result.each { k, v ->
                if (desiredValues.containsKey(k) &&
                        desiredValues.get(k) != v) {
                    log.warn("Bio experiment for study $studyId " +
                            "currently has $k='$v', we would set to " +
                            "'${desiredValues.get(k)}' if we were to create " +
                            "it from scratch.")
                }
            }

            log.info "Found existing bio_experiment with id $result.id"
            result.id as Long
        } catch (EmptyResultDataAccessException erdae) {
            log.debug "Couldn't find existing bio experiment for study $studyId"

            Long id = sequenceReserver.getNext(Sequences.BIO_DATA_ID)
            assert id != null

            def data = [
                    bio_experiment_id: (Object) id,
                    *: desiredValues]
            log.debug("Will insert following data " +
                    "into $Tables.BIO_EXPERIMENT: $data")

            bioExperimentInsert.execute(data)
            log.debug("Created new bio_experiment with id $id")
            id
        }
    }
}
