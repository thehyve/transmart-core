package org.transmartproject.batch.secureobject

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.biodata.BioExperimentDAO
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.SequenceReserver

/**
 * Creation/deletion of secure objects.
 */
@Component
@BackoutComponent
@Slf4j
@TypeChecked
class SecureObjectDAO {

    public static final String CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE = 'BIO_CLINICAL_TRIAL'

    @Autowired
    private SequenceReserver sequenceReserver

    @Value(Tables.SECURE_OBJECT)
    private SimpleJdbcInsert secureObjectInsert

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private BioExperimentDAO bioExperimentDAO

    void createSecureObject(String displayName,
                            String studyId,
                            SecureObjectToken token) {

        long bioExperimentId = bioExperimentDAO.findOrCreateBioExperiment(studyId)
        Map secureObjectValues = findOrCreateSecureObject(
                bioExperimentId, displayName, token)

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

    int deletePermissionsForSecureObject(SecureObjectToken secureObjectToken) {
        jdbcTemplate.update("""
                DELETE FROM ${Tables.SEARCH_AUTH_SEC_OBJ_ACC} ACC
                WHERE EXISTS(SELECT SO.* FROM ${Tables.SECURE_OBJECT} SO
                                WHERE
                                    SO.search_secure_object_id = ACC.secure_object_id
                                    AND SO.data_type = :data_type
                                    AND SO.bio_data_unique_id = :sobj)""",
                [
                        data_type: CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE,
                        sobj: secureObjectToken.toString()])
    }

    int deleteSecureObject(SecureObjectToken secureObjectToken) {
        def secureObject = findSecureObject(secureObjectToken)

        if (!secureObject) {
            log.debug "No existing secure object token $secureObjectToken found"
            return 0
        }

        Long experimentId = (Long) secureObject['bio_data_id']
        if (!experimentId) {
            throw new IllegalStateException('Found entry in ' +
                    "$Tables.SECURE_OBJECT with no reference to an " +
                    "experiment id")
        }

        // biomart.tf_trg_bio_experiment_uid() inserts an entry in
        // biomart.bio_data_uid upon insertion on bio_experiment
        int affected
        log.debug("About to delete bio_experiment with id " +
                "$experimentId from $Tables.BIO_DATA_UID")
        affected = jdbcTemplate.update("""
                DELETE FROM $Tables.BIO_DATA_UID
                WHERE bio_data_id = :id""",
                [id: experimentId])
        log.debug("$affected row(s) affected")
        if (!affected) {
            log.warn("Found no entry with id $experimentId (the experiment " +
                    "id associated witht the secure object) in " +
                    Tables.BIO_DATA_UID)
        }

        log.debug("About to delete bio_experiment with id $experimentId")
        affected = jdbcTemplate.update("""
                DELETE FROM $Tables.BIO_EXPERIMENT
                WHERE bio_experiment_id = :id""",
                [id: experimentId])
        log.debug("$affected row(s) affected")

        if (!affected) {
            log.warn("Found no experiment with id $experimentId, though " +
                    "the secure object $secureObjectToken had a reference " +
                    "to one")
        }

        def secureObjectId = secureObject['search_secure_object_id']
        log.debug("About to delete secure object with id $secureObjectId")
        affected = jdbcTemplate.update("""
                DELETE FROM $Tables.SECURE_OBJECT
                WHERE search_secure_object_id = :id""",
                [id: secureObjectId])
        log.debug("$affected row(s) affected")

        if (affected != 1) {
            throw new IncorrectResultSizeDataAccessException(1)
        }

        affected
    }

    private Map findSecureObject(SecureObjectToken secureObjectToken) {
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
                    "${secureObjectToken.toString()}, but found: $queryResult", 1)
        }

        queryResult.size() > 0 ? queryResult.first() : null
    }

    private Map findOrCreateSecureObject(long experimentId,
                                         String displayName,
                                         SecureObjectToken secureObjectToken) {

        def queryResult = findSecureObject(secureObjectToken)

        def retVal
        if (queryResult == null) {
            Long id = sequenceReserver.getNext(Sequences.SEARCH_SEQ_DATA_ID)
            retVal = [
                    search_secure_object_id: (Object) id,
                    bio_data_unique_id     : secureObjectToken.toString(),
                    data_type              : CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE,
                    display_name           : displayName,
                    bio_data_id            : experimentId,]
            secureObjectInsert.execute(retVal)

            retVal
        } else {
            queryResult
        }
    }
}
