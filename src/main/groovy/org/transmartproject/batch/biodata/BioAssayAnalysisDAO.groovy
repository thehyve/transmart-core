package org.transmartproject.batch.biodata

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.db.SequenceReserver

/**
 * For inserting or updating data in bio_assay_analysis and
 * bio_assay_analysis_ext.
 */
@Component
@Slf4j
class BioAssayAnalysisDAO {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Value(Tables.BIO_ASSAY_ANALYSIS)
    private SimpleJdbcInsert insertBioAssayAnalysis

    @Value(Tables.BIO_ASSAY_ANALYSIS_EXT)
    private SimpleJdbcInsert insertBioAssayAnalysisExt

    @Autowired
    private SequenceReserver sequenceReserver

    Long insertOrUpdateBioAssayAnalysis(Map values) {
        assert values['analysisName'] != null
        assert values['study'] != null
        assert values['dataType'] != null

        long bioAssayAnalysisId = getBioAssayAnalysis(values)
        updateBioAssayAnalysis(bioAssayAnalysisId, values)
        bioAssayAnalysisId
    }

    Long insertOrUpdateBioAssayAnalysisExt(Long bioAssayAnalysisId, Map values) {
        long id = getBioAssayAnalysisExt(bioAssayAnalysisId)
        updateBioAssayAnalysisExt(id, values)
        id
    }

    void updateBioAssayAnalysisCount(Long bioAssayAnalysisId, long count) {
        def affected = jdbcTemplate.update """
                UPDATE ${Tables.BIO_ASSAY_ANALYSIS}
                SET data_count = :count
                WHERE bio_assay_analysis_id = :id""",
                [id: bioAssayAnalysisId, count: count]

        if (affected != 1) {
            throw new IncorrectResultSizeDataAccessException("Expected to " +
                    "update exactly 1 row in $Tables.BIO_ASSAY_ANALYSIS , " +
                    "got $affected rows.")
        }
    }

    private Long getBioAssayAnalysis(Map values) {
        def ids = jdbcTemplate.queryForList """
                SELECT BIO_ASSAY_ANALYSIS_ID
                FROM $Tables.BIO_ASSAY_ANALYSIS
                WHERE
                    ANALYSIS_NAME = :analysisName AND
                    ETL_ID = :study AND
                    BIO_ASSAY_DATA_TYPE = :dataType
        """, values, Long

        if (ids.size() == 1) {
            def id = ids.first()
            log.info "Found existing bio assay analysis, its id is $id"
            return id
        } else if (ids.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(
                    "Expected only one result, got ${ids.size()}: $ids")
        }

        def id = sequenceReserver.getNext(Sequences.BIO_DATA_ID)

        def ret = insertBioAssayAnalysis.execute(
                bio_assay_analysis_id: id,
                analysis_name: values['analysisName'],
                etl_id: values['study'],
                bio_assay_data_type: values['dataType'],
        )
        assert ret == 1
        id
    }

    private void updateBioAssayAnalysis(Long id, Map values) {
        def params = [
                etl_id: values.study,
                bio_assay_data_type: values.dataType,
                analysis_name: values.analysisName,

                long_description: values.description,
                pvalue_cutoff: values.pvalueCutoff,
                analysis_create_date: new Date(),
                short_description: values.analysisNameArchived,
                analysis_type: values.statisticalTest,
        ]

        def frag = params.collect { k, v -> "$k = :$k" }.join(', ')
        def sql = """
                UPDATE ${Tables.BIO_ASSAY_ANALYSIS}
                SET $frag WHERE bio_assay_analysis_id = :id
        """

        log.debug("Will update ${Tables.BIO_ASSAY_ANALYSIS} with values " +
                "$params for id  = $id")
        def ret = jdbcTemplate.update(sql, [id: id, *:params])
        DatabaseUtil.checkUpdateCounts([ret], "updating $Tables.BIO_ASSAY_ANALYSIS")
    }

    private Long getBioAssayAnalysisExt(Long bioAssayAnalysisId) {
        assert bioAssayAnalysisId != null
        def ids = jdbcTemplate.queryForList """
                SELECT BIO_ASSAY_ANALYSIS_EXT_ID
                FROM $Tables.BIO_ASSAY_ANALYSIS_EXT
                WHERE
                    BIO_ASSAY_ANALYSIS_ID = :bio_assay_analysis_id
        """, [bio_assay_analysis_id: bioAssayAnalysisId], Long

        def id
        if (ids.size() == 1) {
            id = ids.first()
            assert id != null
            log.info("Found existing bio_assay_analysis_ext, its is id $id")
            return id
        } else if (ids.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(
                    "Found more than one bio_assay_analysis_ext for " +
                            "bio_assay_analysis with id " +
                            "$bioAssayAnalysisId, their ids are: $ids")
        }

        id = sequenceReserver.getNext(Sequences.BIO_DATA_ID)

        def data = [ bio_assay_analysis_id: bioAssayAnalysisId,
                     bio_assay_analysis_ext_id: id, ]
        log.debug("Inserting bio_assay_analysis_ext with values: $data")
        def ret = insertBioAssayAnalysisExt.execute(data)
        assert ret == 1
        id
    }

    private void updateBioAssayAnalysisExt(Long id, Map values) {
        def params = [
                genome_version: values.genomeVersion,
                tissue: values.tissue,
                cell_type: values.cellType,
                sample_size: values.sampleSize,
                population: values.population,
                model_name: values.modelName,
                model_desc: values.modelDesc,
                research_unit: values.researchUnit,
        ]

        def frag = params.collect { k, v -> "$k = :$k" }.join(', ')
        def sql = """
                UPDATE ${Tables.BIO_ASSAY_ANALYSIS_EXT}
                SET $frag WHERE bio_assay_analysis_ext_id = :id
        """
        log.debug("About to update bio_assay_analysis_ext of id $id with " +
                "values $params")
        def ret = jdbcTemplate.update(sql, [*:params, id: id])
        DatabaseUtil.checkUpdateCounts([ret], "updating $Tables.BIO_ASSAY_ANALYSIS_EXT")
    }
}
