package org.transmartproject.batch.highdim.acgh.data

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
class AcghDataWriter implements ItemWriter<AcghDataValue> {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    @Value("#{acghDataJobContextItems.patientIdAssayIdMap}")
    Map<String, Long> patientIdAssayIdMap

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Value("#{acghDataJobContextItems.partitionTableName}")
    private String qualifiedTableName

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private SimpleJdbcInsert jdbcInsert = {
        new SimpleJdbcInsert(jdbcTemplate)
                .withSchemaName(schemaName(qualifiedTableName))
                .withTableName(tableName(qualifiedTableName))
    }()

    @Override
    void write(List<? extends AcghDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect {

            Long assayId = patientIdAssayIdMap[it.sampleCode]
            if (!assayId) {
                throw new IllegalArgumentException("Passed acgh data value with " +
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
                    //TODO Not implemented yet in the database
                    //probhomloss : it.probHomLoss,
                    probloss  : it.probLoss,
                    probnorm  : it.probNorm,
                    probgain  : it.probGain,
                    probamp   : it.probAmp,
            ]
        } as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting acgh data in $qualifiedTableName")
    }

}
