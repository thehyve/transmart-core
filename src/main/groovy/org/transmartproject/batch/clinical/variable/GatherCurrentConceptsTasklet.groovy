package org.transmartproject.batch.clinical.variable

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.concept.ConceptTree

import java.sql.ResultSet

/**
 * Gets the current concepts (for the study) from database, populating the ConceptTree
 */
@Slf4j
class GatherCurrentConceptsTasklet implements Tasklet {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    ConceptTree conceptTree

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        ConceptPath rootPath = conceptTree.topNodePath

        String sql = '''
                SELECT c_fullname, c_hlevel, c_name, c_basecode, record_id
                FROM i2b2metadata.i2b2
                WHERE sourcesystem_cd = :study OR c_fullname IN (:fullNames)'''

        def rootAndParents = [rootPath.toString()]
        for (def n = rootPath; n != null; n = n.parent) {
            rootAndParents << n.toString()
        }

        List<ConceptNode> concepts = jdbcTemplate.query(
                sql,
                [
                        study: studyId,
                        fullNames: rootAndParents
                ],
                GatherCurrentConceptsTasklet.&resultRowToConceptNode as RowMapper<ConceptNode>)

        concepts.each {
            log.debug('Found existing concept {}', it)
            contribution.incrementReadCount() //increment reads. unfortunately we have to do this in some loop
        }

        conceptTree.loadExisting concepts

        RepeatStatus.FINISHED
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private static ConceptNode resultRowToConceptNode(ResultSet rs, int rowNum) {
        ConceptPath path = new ConceptPath(rs.getString('c_fullname'))
        new ConceptNode(
                level: rs.getInt('c_hlevel'),
                path: path,
                name: rs.getString('c_name'),
                code: rs.getString('c_basecode'),
                i2b2RecordId: rs.getLong('record_id'))
    }
}
