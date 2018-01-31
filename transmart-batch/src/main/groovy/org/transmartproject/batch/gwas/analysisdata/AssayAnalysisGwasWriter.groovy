package org.transmartproject.batch.gwas.analysisdata

import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.db.SequenceReserver

import java.lang.reflect.Field
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Types

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

    @Autowired
    private JdbcTemplate jdbcTemplate

    @Lazy
    List<String> fieldSequence = generateFieldSequence()

    @Value("#{currentGwasAnalysisContext.bioAssayAnalysisId}")
    private Long bioAssayAnalysisId

    @Override
    void write(List<? extends GwasAnalysisRow> items) throws Exception {
        // Unfortunately, SimpleJdbcInsert doesn't work on Oracle
        // p_value and log_p_value are binary_doubles on Oracle.
        // Using new SqlParameterValue(java.sql.Types.DOUBLE, row.pValue)
        // calls setObject() on the prepared statement in StatementCreatorUtils,
        // but the Oracle JDBC driver needs setDouble() (or, better,
        // setBinaryDouble) to be called.
        int[] affected = jdbcTemplate.batchUpdate """
            INSERT INTO $Tables.BIO_ASSAY_ANALYSIS_GWAS(
                    bio_asy_analysis_gwas_id,
                    bio_assay_analysis_id,
                    rs_id,
                    p_value_char,
                    p_value,
                    log_p_value,
                    effect_allele,
                    other_allele,
                    standard_error,
                    beta,
                    ext_data)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                new AssayAnalysisGwasWriterPreparedStatementSetter(items: items)

        DatabaseUtil.checkUpdateCounts(affected,
                "inserting rows in $Tables.BIO_ASSAY_ANALYSIS_GWAS")
    }

    private class AssayAnalysisGwasWriterPreparedStatementSetter
            implements BatchPreparedStatementSetter {

        List<? extends GwasAnalysisRow> items

        @Override
        void setValues(PreparedStatement ps, int i) throws SQLException {
            GwasAnalysisRow row = items[i]

            ps.setLong   1, sequenceReserver.getNext(Sequences.BIO_DATA_ID)
            ps.setLong   2, bioAssayAnalysisId
            ps.setString 3, row.rsId
            ps.setString 4, row.pValue.toPlainString()

            Double pValueDouble = row.pValue.toDouble()
            Double logPValue = - (Math.log(row.pValue) / LOG_LOG_BASE)
            if (ps.respondsTo('setBinaryDouble')) {
                ps.setBinaryDouble 5, pValueDouble
                ps.setBinaryDouble 6, logPValue
            } else {
                ps.setDouble 5, pValueDouble
                ps.setDouble 6, logPValue
            }

            ps.setString 7, row.allele1
            ps.setString 8, row.allele2
            
            if (row.standardError != null) {
                ps.setDouble 9, row.standardError.toDouble()
            } else {
                ps.setNull 9, Types.DOUBLE
            }

            if (row.beta != null) {
                ps.setDouble 10, row.beta.toDouble()
            } else {
                ps.setNull 10, Types.DOUBLE
            }

            ps.setString 11, getExtData(row)
        }

        @Override
        int getBatchSize() {
            items.size()
        }
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
