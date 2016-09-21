package org.transmartproject.batch.facts

import groovy.util.logging.Slf4j
import org.transmartproject.batch.concept.ConceptPath
import org.transmartproject.batch.db.GenericTableUpdateTasklet
import org.transmartproject.batch.support.StringUtils

import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Deletes observation facts (that are not highdim) for a study
 */
@Slf4j
class DeleteObservationFactTasklet extends GenericTableUpdateTasklet {

    boolean highDim = false

    Collection<ConceptPath> basePaths = Collections.emptyList() // optional

    void setBasePath(ConceptPath path) {
        basePaths = [path]
    }

    @Override
    String getSql() {
        def baseNodePart
        if (basePaths) {
            baseNodePart = 'AND (' + basePaths.collect {
                "c_fullname LIKE ? ESCAPE '\\'"
            }.join(" OR ") + ')'
        } else {
            baseNodePart = ''
        }

        def q = """
            DELETE FROM i2b2demodata.observation_fact
            WHERE concept_cd IN (
                SELECT c_basecode FROM i2b2metadata.i2b2
                WHERE sourcesystem_cd = ?
                AND c_visualattributes ${highDim ? '' : 'NOT'} LIKE '__H' ESCAPE '\\'
                $baseNodePart)"""

        log.debug("Query is $q")
        q
    }

    @Override
    void setValues(PreparedStatement ps) throws SQLException {
        ps.setString(1, studyId)
        basePaths.eachWithIndex { ConceptPath basePath, int i ->
            ps.setString(i + 2, StringUtils.escapeForLike(basePath.toString(), '\\'))
        }
    }
}
