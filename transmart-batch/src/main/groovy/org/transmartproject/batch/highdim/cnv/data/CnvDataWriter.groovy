package org.transmartproject.batch.highdim.cnv.data

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
 * Writes the data to de_subject_acgh_data.
 */
@Component
@JobScope
class CnvDataWriter implements ItemWriter<CnvDataValue> {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    @Value("#{cnvDataJobContextItems.patientIdAssayIdMap}")
    Map<String, Long> patientIdAssayIdMap

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{cnvDataJobContextItems.partitionTableName}")
    private String qualifiedTableName

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName(qualifiedTableName))
                .withTableName(tableName(qualifiedTableName))
    }()

    @Override
    void write(List<? extends CnvDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect {

            Long assayId = patientIdAssayIdMap[it.sampleCode]
            if (!assayId) {
                throw new IllegalArgumentException("Passed cnv data value with " +
                        "unknown sample code (${it.sampleCode}). Known ids are " +
                        patientIdAssayIdMap.keySet().sort())
            }
            [
                    trial_name: studyId,
                    region_id : annotationEntityMap[it.regionName],
                    assay_id  : assayId,
                    patient_id: it.patient.code,
                    chip      : it.chip,
                    segmented : it.segmented,
                    flag      : it.flag,
                    probhomloss : it.probHomLoss,
                    probloss  : it.probLoss,
                    probnorm  : it.probNorm,
                    probgain  : it.probGain,
                    probamp   : it.probAmp,
            ]
        } as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting cnv data in $qualifiedTableName")
    }

}
