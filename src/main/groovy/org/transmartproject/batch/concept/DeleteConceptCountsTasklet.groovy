package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import org.transmartproject.batch.db.GenericTableUpdateTasklet
import org.transmartproject.batch.support.StringUtils

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Deletes concept counts for a study</br>
 * This will delete counts for all kinds of leaf concepts (both lowdim and highdim)
 */
@Slf4j
class DeleteConceptCountsTasklet extends GenericTableUpdateTasklet {

    ConceptPath basePath

    final String sql = '''
        DELETE
        FROM i2b2demodata.concept_counts
        WHERE concept_path IN (
            SELECT c_fullname
            FROM i2b2metadata.i2b2
            WHERE
                sourcesystem_cd = ?
                AND c_fullname LIKE ? ESCAPE '\\'
            )
        '''

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("About to delete concept counts for study $studyId, base node $basePath")
        super.execute(contribution, chunkContext)
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
        ps.setString(2, StringUtils.escapeForLike(basePath.toString()))
    }
}
