package org.transmartproject.batch.highdim.assays

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowMapper
import org.transmartproject.batch.backout.BackoutComponent
import org.transmartproject.batch.clinical.db.objects.Tables

import javax.annotation.PostConstruct
import javax.sql.DataSource
import java.sql.ResultSet

import static org.transmartproject.batch.support.StringUtils.escapeForSQLString

/**
 * Reads assay ids for the current study, possibly with a certain
 * platform type.
 *
 * Unlike {@link CurrentAssayIdsReader}, it doesn't use the concept tree to
 * limit the assay search. Perhaps these could be merged together or at least
 * common code put in another component.
 */
@BackoutComponent
@JobScope
class BackoutAssayIdReader implements ItemStreamReader<Long> {

    @Autowired
    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    DataSource dataSource

    String platformType // leave null to select all

    @PostConstruct
    void init() {
        delegate = new JdbcCursorItemReader<Long>(
                driverSupportsAbsolute: true,
                dataSource: dataSource,
                sql: sql,
                rowMapper: this.&mapRow as RowMapper<Long>)

        delegate.afterPropertiesSet()
    }

    @Delegate
    JdbcCursorItemReader<Long> delegate

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private Long mapRow(ResultSet rs, int rowNum) {
        rs.getLong(1)
    }

    private String getSql() {
        def studyEscaped = escapeForSQLString(studyId)
        def res = """
                SELECT assay_id
                FROM ${Tables.SUBJ_SAMPLE_MAP} SSM
                WHERE SSM.trial_name = '$studyEscaped'"""
        if (platformType) {
            res += " AND platform = '${escapeForSQLString(platformType)}'"
        }

        res
    }
}
