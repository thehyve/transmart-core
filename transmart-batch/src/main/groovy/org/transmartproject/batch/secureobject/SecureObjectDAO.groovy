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
import org.transmartproject.batch.facts.ClinicalFactsRowSet

/**
 * Creation/deletion of secure objects.
 */
@Component
@BackoutComponent
@Slf4j
@TypeChecked
class SecureObjectDAO {

    public static final String CLINICAL_TRIAL_SECURE_OBJECT_DATA_TYPE = 'BIO_CLINICAL_TRIAL'
    public static final String DUMMY_SECURITY_CONCEPT_CD = 'SECURITY'

    @Autowired
    private SequenceReserver sequenceReserver

    @Value(Tables.SECURE_OBJECT)
    private SimpleJdbcInsert secureObjectInsert

    @Value(Tables.OBSERVATION_FACT)
    private SimpleJdbcInsert dummySecurityObservationsInsert

    @Value(Tables.STUDY)
    private SimpleJdbcInsert studyInsert

    @Value(Tables.STUDY_DIM_DESCRIPTIONS)
    private SimpleJdbcInsert studyDimensionDescriptionsInsert

    @Value(Tables.DIMENSION_DESCRIPTION)
    private SimpleJdbcInsert dimensionDescriptionInsert

    @Value(Tables.MODIFIER_DIM)
    private SimpleJdbcInsert modifierInsert

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    private BioExperimentDAO bioExperimentDAO

    void createSecureObject(String displayName,
                            SecureObjectToken token,
                            boolean hasOntologyMapping) {

        // Find or create bio experiment object
        long bioExperimentId = bioExperimentDAO.findOrCreateBioExperiment(token.studyId)

        // Find or create study object
        Map studyValues = findOrCreateStudy(token.studyId, token, bioExperimentId, hasOntologyMapping)
        if (studyValues['secure_obj_token'] != token.toString()) {
            throw new IllegalStateException("Study found " +
                    "($studyValues) does not have expected " +
                    "secure object token ${token.toString()}")
        }

        // Find or create secure object
        Map secureObjectValues = findOrCreateSecureObject(
                bioExperimentId, displayName, token)
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

        if (!token.public) {
            insertDummySecurityObservation(token)
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
                        sobj     : secureObjectToken.toString()])
    }

    int deleteSecureObject(SecureObjectToken secureObjectToken) {
        if (!secureObjectToken?.experimentId) {
            log.debug "No experiment id found for secure object token $secureObjectToken"
            return 0
        }
        def secureObject = findSecureObjectByExperimentId(secureObjectToken.experimentId.longValue())

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

        log.debug("About to delete ${Tables.STUDY_DIM_DESCRIPTIONS} for bio_experiment_id=${experimentId}")
        affected = jdbcTemplate.update("""
                DELETE FROM ${Tables.STUDY_DIM_DESCRIPTIONS}
                WHERE study_id IN (
                  SELECT study_num FROM ${Tables.STUDY}
                  WHERE bio_experiment_id = :id)""",
                [id: experimentId])
        log.debug("$affected row(s) affected")

        log.debug("About to delete ${Tables.STUDY} with bio_experiment_id=${experimentId}")
        affected = jdbcTemplate.update("""
                DELETE FROM ${Tables.STUDY}
                WHERE bio_experiment_id = :id""",
                [id: experimentId])
        log.debug("$affected row(s) affected")

        if (!affected) {
            log.warn("Found no studies with id ${experimentId}")
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

        deleteDummySecurityObservation(secureObjectToken)

        affected
    }

    private Map findSecureObjectByExperimentId(long experimentId) {
        def queryResult = jdbcTemplate.queryForList """
                SELECT bio_data_id,
                        display_name,
                        data_type,
                        bio_data_unique_id,
                        search_secure_object_id
                FROM ${Tables.SECURE_OBJECT}
                WHERE bio_data_id = :experimentId
                """, [experimentId: experimentId]

        if (queryResult.size() > 1) {
            throw new IncorrectResultSizeDataAccessException("Expected to get " +
                    "only one search secure object with bio_data_id = " +
                    "${experimentId}, but found: $queryResult", 1)
        }

        queryResult.size() > 0 ? queryResult.first() : null
    }

    private Map findOrCreateSecureObject(long experimentId,
                                         String displayName,
                                         SecureObjectToken secureObjectToken) {

        def queryResult = findSecureObjectByExperimentId(experimentId)

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
            secureObjectToken.experimentId = id
            retVal
        } else {
            secureObjectToken.experimentId = experimentId
            queryResult
        }
    }

    /**
     * Inserts the dummy "SECURITY" fact for the study to prevent existing kettle ETL opening up access to the study.
     * The original purpose of having such observations is unknown.
     * The sql insert query that causing the issue could be found here
     * https://github.com/transmart/transmart-data
     * /blob/cd7f0e337c98a261336cc4ed1db15590beeec316/ddl/postgres/tm_cz/functions/i2b2_load_security_data.sql#L104
     */
    private int insertDummySecurityObservation(SecureObjectToken token) {
        def row = [
                sourcesystem_cd: token.studyId,
                encounter_num  : -1,
                patient_num    : -1,
                concept_cd     : DUMMY_SECURITY_CONCEPT_CD,
                valtype_cd     : 'T',
                tval_char      : token.toString(),
                start_date     : new GregorianCalendar(1, 0, 1).time,
                import_date    : new Date(),
                provider_id    : '@',
                location_cd    : '@',
                modifier_cd    : token.studyId,
                valueflag_cd   : '@',
                instance_num   : 1,
        ] as Map<String, Object>

        dummySecurityObservationsInsert.execute(row)
    }

    private int deleteDummySecurityObservation(SecureObjectToken token) {
        jdbcTemplate.update("""
                DELETE FROM ${Tables.OBSERVATION_FACT}
                WHERE sourcesystem_cd = :studyId and concept_cd = :conceptCd""",
                [
                        studyId  : token.studyId,
                        conceptCd: DUMMY_SECURITY_CONCEPT_CD,
                ])
    }

    private Map findStudy(String studyId) {
        def queryResult = jdbcTemplate.queryForList """
                SELECT study_num,
                        study_id,
                        secure_obj_token,
                        bio_experiment_id
                FROM ${Tables.STUDY}
                WHERE study_id = :studyId
                """, [studyId: studyId]

        if (queryResult.size() > 1) {
            throw new IncorrectResultSizeDataAccessException("Expected to get " +
                    "only one study with study_id = " +
                    "${studyId}, but found: $queryResult", 1)
        }

        queryResult.size() > 0 ? queryResult.first() : null
    }

    private Map findOrCreateStudy(String studyId, SecureObjectToken secureObjectToken,
                                  Long experimentId, boolean hasOntologyMapping) {
        def study = findStudy(studyId)
        if (study != null) {
            return study
        }
        Long id = sequenceReserver.getNext(Sequences.STUDY)
        String token = secureObjectToken.toString()
        study = [
                study_num           : id,
                study_id            : studyId,
                secure_obj_token    : token,
                bio_experiment_id   : experimentId] as Map
        studyInsert.execute(study)
        log.info "Inserted new study: ${study.toMapString()}"
        createStudyDimensionDescriptions(id, hasOntologyMapping)
        study
    }

    public static final List<String> BUILT_IN_DIMENSIONS = [
            "study",
            "concept",
            "patient",
            "visit",
            "start time",
            "end time",
            "location",
            "trial visit",
            "provider",
            "biomarker",
            "assay",
            "projection"
    ]

    public static final String ORIGINAL_VARIABLE_DIMENSION_NAME = 'original_variable'

    /**
     * Find or create built-in dimensions {@link #BUILT_IN_DIMENSIONS} and
     * a dimension {@link #ORIGINAL_VARIABLE_DIMENSION_NAME} that contains original variable names,
     * used when generic ontology codes are replacing variable names at data loading time.
     * The ontology codes are used as concept codes, the original variable names
     * are stored using the modifier with code {@link ClinicalFactsRowSet#ORIGINAL_VARIABLE_NAME_MODIFIER}.
     */
    private List createStudyDimensionDescriptions(Long studyNum, boolean hasOntologyMapping) {
        List<Long> dimensionIds = []
        for (String dimensionName: BUILT_IN_DIMENSIONS) {
            def dimension = findOrCreateDimension(dimensionName, null)
            dimensionIds << (dimension.id as Long)
        }
        if (hasOntologyMapping) {
            findOrCreateOriginalVariableModifier()
            def originalVariableDimension = findOrCreateDimension(
                    ORIGINAL_VARIABLE_DIMENSION_NAME, ClinicalFactsRowSet.ORIGINAL_VARIABLE_NAME_MODIFIER)
            dimensionIds << (originalVariableDimension.id as Long)
        }
        dimensionIds.each { dimensionId ->
            studyDimensionDescriptionsInsert.execute([
                    dimension_description_id: dimensionId,
                    study_id: studyNum
            ] as Map)
        }
        log.info "Inserted dimension descriptions for study: ${dimensionIds.size()}."
    }

    private Map findDimension(String dimensionName) {
        def queryResult = jdbcTemplate.queryForList """
                SELECT id,
                    modifier_code,
                    value_type,
                    name,
                    density,
                    packable,
                    size_cd
                FROM ${Tables.DIMENSION_DESCRIPTION}
                WHERE name = :name
                """, [name: dimensionName]

        if (queryResult.size() > 1) {
            throw new IncorrectResultSizeDataAccessException("Expected to get " +
                    "only one dimension description with name = " +
                    "${dimensionName}, but found: $queryResult", 1)
        }

        queryResult.size() > 0 ? queryResult.first() : null
    }

    /**
     * Find or create a dimension with the specified name. For non built-in
     * dimensions, a modifier is expected.
     */
    private Map findOrCreateDimension(String dimensionName, String modifierCode) {
        def dimension = findDimension(dimensionName)
        if (dimension != null) {
            return dimension
        }
        Long id = sequenceReserver.getNext(Sequences.DIMENSION_DESCRIPTION)
        dimension = [
                id: id,
                name: dimensionName,
                modifier_code: modifierCode,
                value_type: modifierCode ? 'T' : null,
                density: modifierCode ? 'DENSE' : null,
                packable: modifierCode ? 'NOT_PACKABLE' : null,
                size_cd: modifierCode ? 'SMALL' : null
        ] as Map
        dimensionDescriptionInsert.execute(dimension)
        log.info "Inserted new dimension description: ${dimension.toMapString()}"
        dimension
    }

    private Map findOriginalVariableModifier() {
        def queryResult = jdbcTemplate.queryForList """
                SELECT modifier_path,
                    modifier_cd,
                    name_char,
                    modifier_blob,
                    update_date,
                    download_date,
                    import_date,
                    sourcesystem_cd,
                    upload_id,
                    modifier_level,
                    modifier_node_type
                FROM ${Tables.MODIFIER_DIM}
                WHERE modifier_cd = :modifier_cd
                """, [modifier_cd: ClinicalFactsRowSet.ORIGINAL_VARIABLE_NAME_MODIFIER]

        if (queryResult.size() > 1) {
            throw new IncorrectResultSizeDataAccessException("Expected to get " +
                    "only one modifier with code = " +
                    "${ClinicalFactsRowSet.ORIGINAL_VARIABLE_NAME_MODIFIER}, but found: $queryResult", 1)
        }

        queryResult.size() > 0 ? queryResult.first() : null
    }

    private Map findOrCreateOriginalVariableModifier() {
        def originalVariableModifier = findOriginalVariableModifier()
        if (originalVariableModifier != null) {
            return originalVariableModifier
        }
        originalVariableModifier = [
                modifier_path: '\\' + ClinicalFactsRowSet.ORIGINAL_VARIABLE_NAME_MODIFIER,
                modifier_cd: ClinicalFactsRowSet.ORIGINAL_VARIABLE_NAME_MODIFIER,
                name_char: ORIGINAL_VARIABLE_DIMENSION_NAME,
                import_date: new Date(),
        ] as Map
        modifierInsert.execute(originalVariableModifier)
        log.info "Inserted new modifier: ${originalVariableModifier.toMapString()}"
        originalVariableModifier
    }

}
