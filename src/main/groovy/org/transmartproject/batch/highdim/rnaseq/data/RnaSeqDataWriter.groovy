package org.transmartproject.batch.highdim.rnaseq.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

import static org.transmartproject.batch.clinical.db.objects.Tables.schemaName
import static org.transmartproject.batch.clinical.db.objects.Tables.tableName

/**
 * Writes the data to de_subject_rnaseq_data.
 */
@Component
@JobScope
class RnaSeqDataWriter implements ItemWriter<RnaSeqDataValue> {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    @Value("#{rnaSeqDataJobContextItems.patientIdAssayIdMap}")
    Map<String, Long> patientIdAssayIdMap

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{rnaSeqDataJobContextItems.partitionTableName}")
    private String qualifiedTableName

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName(qualifiedTableName))
                .withTableName(tableName(qualifiedTableName))
    }()

    @Override
    void write(List<? extends RnaSeqDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect {

            Long assayId = patientIdAssayIdMap[it.sampleCode]
            if (!assayId) {
                throw new IllegalArgumentException("Passed rnaseq data value with " +
                        "unknown sample code (${it.sampleCode}). Known ids are " +
                        patientIdAssayIdMap.keySet().sort())
            }
            [
                    trial_name               : studyId,
                    region_id                : annotationEntityMap[it.annotation],
                    assay_id                 : assayId,
                    patient_id               : it.patient.code,
                    readcount                : it.readCount,
                    normalized_readcount     : it.value,
                    log_normalized_readcount : it.logValue,
                    zscore                   : it.zscore,
            ]
        } as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting rnaseq data in $qualifiedTableName")
    }

}
