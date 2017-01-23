package org.transmartproject.batch.clinical.xtrial

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptFragment
import org.transmartproject.batch.concept.ConceptType
import org.transmartproject.batch.db.DatabaseUtil

import java.sql.ResultSet

/**
 * Repository for loading {@link XtrialNode} objects.
 */
@Component
@JobScope
@Slf4j
class XtrialNodeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate

    List<XtrialNode> getSubTree(ConceptFragment... prefixes) {
        log.debug "Searching for xtrial nodes with prefixes: $prefixes"

        if (!prefixes) {
            return []
        }

        def wherePart = (["modifier_path LIKE ? ESCAPE '\\'"] * prefixes.length)
                .join(' OR ')
        jdbcTemplate.query("""
                SELECT modifier_path, modifier_cd, valtype_cd
                FROM ${Tables.MODIFIER_DIM_VIEW}
                WHERE $wherePart""",
                { ResultSet rs, int rowNum ->
                    new XtrialNode(
                            code: rs.getString('modifier_cd'),
                            path: new ConceptFragment(rs.getString('modifier_path')),
                            type: rs.getString('valtype_cd') == 'N' ?
                                    ConceptType.NUMERICAL : ConceptType.CATEGORICAL
                    )
                } as RowMapper,
                prefixes.collect {
                    DatabaseUtil.asLikeLiteral(it.toString()) + '%'
                } as Object[])
    }
}
