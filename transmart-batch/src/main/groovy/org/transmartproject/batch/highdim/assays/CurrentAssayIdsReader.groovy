package org.transmartproject.batch.highdim.assays

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.concept.ConceptType

import javax.annotation.PostConstruct
import javax.sql.DataSource
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Finds assays associated with the concept paths for this job.
 */
@Component
@JobScope
@Slf4j
class CurrentAssayIdsReader implements ItemStreamReader<Long> {

    @Value("#{jobExecutionContext['gatherCurrentTreeNodesTasklet.listOfConcepts']}")
    Collection<ConceptPath> conceptPaths

    @Autowired
    ConceptTree conceptTree

    @Autowired
    private DataSource dataSource

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Collection<ConceptNode> oldHighDimNodes = { ->
        conceptPaths
                .collect { conceptTree[it] } /* ConceptNodes */
                .findAll { it.type == ConceptType.HIGH_DIMENSIONAL }
                .findAll { conceptTree.isSavedNode(it) }
    }()

    @Delegate
    JdbcCursorItemReader<Long> delegate

    @PostConstruct
    void init() {
        def wherePart
        def parameters

        if (!oldHighDimNodes) {
            log.debug('Nothing to do, no old high dim nodes ' +
                    'related to current job')

            wherePart = '1=2'
            parameters = []
        } else {
            log.info('Will delete assays and data related with concepts: ' +
                    oldHighDimNodes)

            wherePart = """SSM.concept_code IN (${oldHighDimNodes.collect { '?' }.join(', ')})"""
            parameters = oldHighDimNodes*.code
        }

        delegate = new JdbcCursorItemReader(dataSource: dataSource)

        name = getClass().simpleName

        sql = """
                SELECT assay_id
                FROM ${Tables.SUBJ_SAMPLE_MAP} SSM
                WHERE $wherePart"""
        preparedStatementSetter = { PreparedStatement ps ->
            parameters.eachWithIndex { String code, int index ->
                ps.setString(index + 1 /* 1-based */, code)
            }
        } as PreparedStatementSetter

        rowMapper = { ResultSet rs, int rowNum ->
            rs.getLong(1)
        } as RowMapper<Long>
    }
}
