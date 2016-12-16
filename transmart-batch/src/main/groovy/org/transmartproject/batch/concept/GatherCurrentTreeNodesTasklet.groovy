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
import org.transmartproject.batch.clinical.ontology.OntologyMapping
import org.xml.sax.SAXException

import java.sql.ResultSet

/**
 * Gets the current concepts from database, populating the ConceptTree.
 * It will always load the TOP_NODE for the study.
 *
 * If the job context variable
 * {@link GatherCurrentTreeNodesTasklet#LIST_OF_CONCEPTS_KEY}
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
class GatherCurrentTreeNodesTasklet implements Tasklet {

    public final static String LIST_OF_CONCEPTS_KEY = 'gatherCurrentTreeNodesTasklet.listOfConcepts'

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Value("#{jobExecution.executionContext.get('gatherCurrentTreeNodesTasklet.listOfConcepts')}")
    Collection<ConceptPath> conceptPaths

    @Autowired
    ConceptTree conceptTree

    @Autowired
    OntologyMapping ontologyMapping

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        String sql = '''
                SELECT c_fullname, c_hlevel, c_name, c_dimcode, c_metadataxml, c_visualattributes
                FROM i2b2metadata.i2b2
                WHERE
                c_tablename LIKE 'CONCEPT_DIMENSION' AND
                c_columnname LIKE 'CONCEPT_PATH' AND
                c_operator IN ('=', 'LIKE') AND (
                c_fullname IN (:rootPathFullNames) OR '''

        def params = [
                rootPathFullNames: thisAndItsParents(conceptTree.topNodePath),
        ]

        log.debug("Will look for concepts ${params.rootPathFullNames}. " +
                    "List originally given: $conceptPaths")

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
        sql += ')'

        List<ConceptNode> treeNodes = jdbcTemplate.query(
                sql,
                params,
                GatherCurrentTreeNodesTasklet.&resultRowToConceptNode as RowMapper<ConceptNode>)

        treeNodes.each {
            log.debug "Found existing i2b2 node ${it.path}"
            if (it.conceptPath) {
                def conceptCode = jdbcTemplate.query(
                        "select CONCEPT_CD from I2B2DEMODATA.CONCEPT_DIMENSION where CONCEPT_PATH = :path",
                        [path: it.conceptPath.toString()],
                        { ResultSet rs, int rowNum -> rs.getString('CONCEPT_CD') } as RowMapper<String>
                )
                if (!conceptCode.empty) {
                    it.code = conceptCode[0]
                    log.debug "Concept code is ${it.code}."
                } else {
                    log.warn "No concept code found."
                }
                def ontologyNode = ontologyMapping.getNodeForPath(it.path)
                if (ontologyNode) {
                    it.ontologyNode = true
                }
            }
            contribution.incrementReadCount() //increment reads. unfortunately we have to do this in some loop
        }

        conceptTree.loadExisting treeNodes

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
        def path = new ConceptPath(rs.getString('c_fullname'))
        def dimCode = rs.getString('c_dimcode')
        def conceptPath = (dimCode && !dimCode.empty) ? new ConceptPath(dimCode) : null
        new ConceptNode(
                level: rs.getInt('c_hlevel'),
                path: path,
                name: rs.getString('c_name'),
                conceptName: null,
                conceptPath: conceptPath,
                code: null,
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
