package org.transmartproject.batch.clinical.secureobject

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.support.SecureObjectToken

/**
 * If the study is private, insert the necessary data into bio_experiment and
 * search_secure_object.
 */
@Component
@JobScopeInterfaced
@Slf4j
class CreateSecureStudyTasklet implements Tasklet {

    public static final String CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE = 'BIO_CLINICAL_TRIAL'

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNodePath

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    SecureObjectToken secureObjectToken

    @Autowired
    private SequenceReserver sequenceReserver

    @Value(Tables.BIO_EXPERIMENT)
    SimpleJdbcInsert bioExperimentInsert

    @Value(Tables.SECURE_OBJECT)
    SimpleJdbcInsert secureObjectInsert

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Override
    RepeatStatus execute(StepContribution contribution,
                         ChunkContext chunkContext) throws Exception {

        if (secureObjectToken.toString() == 'EXP:PUBLIC') {
            log.info("Study is public; will not take any action " +
                    "(existing secure objects will NOT be deleted)")
            return RepeatStatus.FINISHED
        }

        long bioExperimentId = findOrCreateBioExperiment()
        Map secureObjectValues = findOrCreateSecureObject(bioExperimentId)

        /* some quick validation */
        if (secureObjectValues['bio_data_id'] != bioExperimentId) {
            throw new IllegalStateException("Secure object found " +
                    "($secureObjectValues) does not point to expected " +
                    "experiment id $bioExperimentId")
        }
        if (secureObjectValues['data_type'] !=
                CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE) {
            throw new IllegalStateException("Expected data type of found " +
                    "existing secure object to be " +
                    "$CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE, but got " +
                    "$secureObjectValues")
        }
    }

    private Long findOrCreateBioExperiment() {
        try {
            def id = jdbcTemplate.queryForObject """
                    SELECT bio_experiment_id
                    FROM ${Tables.BIO_EXPERIMENT}
                    WHERE accession = :study""",
                    [study: studyId],
                    Long

            log.info "Found existing bio_experiment with id $id"
            id
        } catch (EmptyResultDataAccessException erdae) {
            Long id = sequenceReserver.getNext(Sequences.BIO_DATA_ID)

            bioExperimentInsert.execute(
                    bio_experiment_id: id,
                    title:             'Metadata not available',
                    accession:         studyId,
                    etl_id:            "METADATA:$studyId" as String
            )
            log.debug("Created new bio_experiment with id $id")

            id
        }
    }

    private Map findOrCreateSecureObject(long experimentId) {
        def queryResult = jdbcTemplate.queryForList """
                SELECT bio_data_id,
                        display_name,
                        data_type,
                        bio_data_unique_id,
                        search_secure_object_id
                FROM ${Tables.SECURE_OBJECT}
                WHERE bio_data_unique_id = :sot
                """, [sot: secureObjectToken.toString()]

        if (queryResult.size() > 1) {
            throw new IncorrectResultSizeDataAccessException("Expected to get " +
                    "only one search secure object with bio_data_unique_id = " +
                    "${secureObjectToken.toString()}, but found: $queryResult")
        }

        def retVal
        if (queryResult.size() == 0) {
            Long id = sequenceReserver.getNext(Sequences.SEARCH_SEQ_DATA_ID)
            retVal = [
                    search_secure_object_id: id,
                    bio_data_unique_id: secureObjectToken.toString(),
                    data_type: CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE,
                    display_name: displayName,
                    bio_data_id: experimentId,]
            secureObjectInsert.execute(retVal)

            retVal
        } else {
            queryResult.first()
        }
    }

    private String getDisplayName() {
        if (topNodePath.length >= 2) {
            /* Not great logic, but it's what the stored procedures currently have */
            "${topNodePath[0]} - $studyId"
        } else {
            studyId
        }
    }
}
