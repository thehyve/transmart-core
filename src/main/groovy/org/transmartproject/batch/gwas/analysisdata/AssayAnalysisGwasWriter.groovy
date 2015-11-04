package org.transmartproject.batch.gwas.analysisdata

import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.db.SequenceReserver

import java.lang.reflect.Field

/**
 * Inserts rows into bio_assay_analysis_gwas.
 */
@Component
@StepScope
class AssayAnalysisGwasWriter implements ItemWriter<GwasAnalysisRow> {

    private static final int LOG_BASE = 10
    private static final double LOG_LOG_BASE = Math.log(LOG_BASE)

    @Autowired
    private SequenceReserver sequenceReserver

    @Value(Tables.BIO_ASSAY_ANALYSIS_GWAS)
    private SimpleJdbcInsert jdbcInsert

    @Lazy
    List<String> fieldSequence = generateFieldSequence()

    @Value("#{currentGwasAnalysisContext.bioAssayAnalysisId}")
    private Long bioAssayAnalysisId

    @Override
    void write(List<? extends GwasAnalysisRow> items) throws Exception {
        int[] affected = jdbcInsert.executeBatch(
                items.collect(this.&convertAnalysisRow) as Map[])
        DatabaseUtil.checkUpdateCounts(affected,
                "inserting rows in ${jdbcInsert.tableName}")
    }

    private Map<String, ?> convertAnalysisRow(GwasAnalysisRow row) {
        [
                bio_asy_analysis_gwas_id: sequenceReserver.getNext(Sequences.BIO_DATA_ID),
                bio_assay_analysis_id:    bioAssayAnalysisId,
                rs_id:                    row.rsId,
                p_value_char:             row.pValue.toPlainString(),
                p_value:                  row.pValue.toDouble(),
                log_p_value:              - (Math.log(row.pValue) / LOG_LOG_BASE),
                ext_data:                 getExtData(row),
        ]
    }

    private List<String> generateFieldSequence() {
        (GwasAnalysisRow.declaredFields as List).findAll { Field it ->
            it.getAnnotation(Order) != null
        }.sort { Field it ->
            it.getAnnotation(Order).value()
        }*.name
    }

    private String getExtData(GwasAnalysisRow row) {
        fieldSequence.collect {
            String v = row."$it"
            v == null ? '' : v
        }.join(';')
    }
}
