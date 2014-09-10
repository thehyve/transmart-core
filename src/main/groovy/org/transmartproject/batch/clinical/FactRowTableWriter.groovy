package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate

import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp

/**
 *
 */
class FactRowTableWriter implements ItemWriter<FactRowSet> {

    static final String TABLE = 'tm_lz.lt_src_clinical_data'
    @Autowired
    private JdbcTemplate jdbcTemplate

    static final SQL = "INSERT INTO $TABLE (study_id, site_id, subject_id, visit_name, data_label, modifier_cd," +
            "data_value, units_cd, date_timestamp, category_cd, ctrl_vocab_code) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

    @Override
    void write(List<? extends FactRowSet> items) throws Exception {

        FactRowBatchSetter setter = new FactRowBatchSetter()
        setter.ts = new Timestamp(System.currentTimeMillis()) //same time for all in this batch
        items.each {
            setter.rows = it.factRows
            jdbcTemplate.batchUpdate(SQL, setter) //one batch update per FactRowSet
        }
    }
}

class FactRowBatchSetter implements BatchPreparedStatementSetter {
    List<FactRow> rows
    Timestamp ts

    @Override
    void setValues(PreparedStatement ps, int i) throws SQLException {
        int j = 1
        FactRow row = rows[i]
        ps.setString(j++, row.studyId)
        ps.setString(j++, row.siteId)
        ps.setString(j++, row.subjectId)
        ps.setString(j++, row.visitName)
        ps.setString(j++, row.dataLabel)
        ps.setString(j++, null) //modifier_cd
        ps.setString(j++, row.value)
        ps.setString(j++, null) //units_cd
        ps.setTimestamp(j++, ts)
        ps.setString(j++, row.categoryCode)
        ps.setString(j++, null) //ctrl_vocab_code
    }

    @Override
    int getBatchSize() {
        return rows.size()
    }
}
