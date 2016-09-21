package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.xml.sax.SAXException

import java.sql.ResultSet

/**
 * Gets the current concepts from database, populating the ConceptTree.
 * It will always load the TOP_NODE for the study.
 *
 * If the job context variable
 * {@link GatherCurrentConceptsTasklet#LIST_OF_CONCEPTS_KEY}
 * is set, then the concepts with full names in that list (and their
 * parents will be loaded). Otherwise, all the concepts for the study will be
 * loaded.
 *
 * This should be on an allowStartIfComplete step, as the ConceptTree is not
 * persisted on the job context.
 */
@Component
@JobScopeInterfaced
@Slf4j
class GatherCurrentConceptsTasklet implements Tasklet {

    public final static String LIST_OF_CONCEPTS_KEY = 'gatherCurrentConceptsTasklet.listOfConcepts'

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobExecution.executionContext.get('gatherCurrentConceptsTasklet.listOfConcepts')}")
    Collection<ConceptPath> conceptPaths

    @Autowired
    ConceptTree conceptTree

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        String sql = '''
                SELECT c_fullname, c_hlevel, c_name, c_basecode, c_metadataxml, c_visualattributes
                FROM i2b2metadata.i2b2
                WHERE c_fullname IN (:rootPathFullNames) OR '''

        def params = [
                rootPathFullNames: thisAndItsParents(conceptTree.topNodePath),
        ]

        if (log.debugEnabled) {
            log.debug("Will look for concepts ${params.rootPathFullNames}. " +
                    "List originally given: $conceptPaths")
        }

        if (conceptPaths) {
            sql += 'c_fullname IN (:fullNames)'
            def fullNames = conceptPaths.collectMany {
                thisAndItsParents(it)
            }
            params['fullNames'] = fullNames
            log.debug('Concept path list given, will look for {} (those ' +
                    'given in {} plus their parents)', fullNames, conceptPaths)
        } else {
            sql += 'sourcesystem_cd = :study'
            params['study'] = studyId
            log.debug('No concept list given; will load all ' +
                    'the concepts for study {}', studyId)
        }

        List<ConceptNode> concepts = jdbcTemplate.query(
                sql,
                params,
                GatherCurrentConceptsTasklet.&resultRowToConceptNode as RowMapper<ConceptNode>)

        concepts.each {
            log.debug('Found existing concept {}', it)
            contribution.incrementReadCount() //increment reads. unfortunately we have to do this in some loop
        }

        conceptTree.loadExisting concepts

        RepeatStatus.FINISHED
    }

    private static List<String> thisAndItsParents(ConceptPath path) {
        def ret = [path.toString()]
        for (def n = path; n != null; n = n.parent) {
            ret << n.toString()
        }
        ret
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private static ConceptNode resultRowToConceptNode(ResultSet rs, int rowNum) {
        ConceptPath path = new ConceptPath(rs.getString('c_fullname'))
        new ConceptNode(
                level: rs.getInt('c_hlevel'),
                path: path,
                name: rs.getString('c_name'),
                code: rs.getString('c_basecode'),
                type: typeFor(rs),
        )
    }

    private static ConceptType typeFor(ResultSet rs) {
        String visualAttributes = rs.getString('c_visualattributes')
        if (visualAttributes.size() == 3 && visualAttributes[2] == 'H') {
            return ConceptType.HIGH_DIMENSIONAL
        }

        String metadataXml = rs.getString('c_metadataxml')
        if (!metadataXml) {
            return ConceptType.CATEGORICAL
        }

        def slurper
        try {
            slurper = new XmlSlurper().parseText(metadataXml)
        } catch (SAXException sex) {
            return ConceptType.CATEGORICAL
        }

        slurper.Oktousevalues == 'Y' ?
                ConceptType.NUMERICAL :
                ConceptType.CATEGORICAL
    }
}
