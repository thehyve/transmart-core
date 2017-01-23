package org.transmartproject.batch.highdim.platform.annotationsload

import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.transmartproject.batch.beans.JobScopeInterfaced

import javax.annotation.PostConstruct
import javax.sql.DataSource
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Reads a platform table from the database and returns mappings between
 * internal (long) ids and logical names for annotation entities.
 * Should be a job scoped bean, but it's not marked with @Component because
 * some properties need to be explicitly set.
 */
@JobScopeInterfaced
class GatherAnnotationEntityIdsReader implements ItemStreamReader<AnnotationEntity> {

    @Value("#{jobExecutionContext['platform.id']}") // sync PlatformJobContextKeys.PLATFORM
    String platform

    String table
    String idColumn
    String nameColumn
    String gplIdColumn = 'gpl_id'

    @Delegate
    JdbcCursorItemReader<AnnotationEntity> delegate

    @Autowired
    DataSource dataSource

    @PostConstruct
    void init() {
        delegate = new JdbcCursorItemReader<>(
                sql: sql,
                dataSource: dataSource,
                preparedStatementSetter: { PreparedStatement ps ->
                    ps.setString(1, platform)

                } as PreparedStatementSetter,
                rowMapper: new AnnotationEntityRowMapper(),
                saveState: false,
        )
    }

    private String getSql() {
        "SELECT $idColumn AS id, $nameColumn AS name " +
                "FROM $table WHERE $gplIdColumn = ?"
    }

    static class AnnotationEntityRowMapper implements RowMapper<AnnotationEntity> {
        @Override
        AnnotationEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            new AnnotationEntity(
                    internalId: rs.getLong(1),
                    name: rs.getString(2),
            )
        }
    }
}
