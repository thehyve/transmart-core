package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.ColumnSpecification
import org.transmartproject.batch.db.DatabaseMetaDataService
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.secureobject.SecureObjectToken

import javax.annotation.PostConstruct

/**
 * Service for inserting the concepts
 */
@Component
@JobScope
@Slf4j
class InsertConceptsService {

    @Autowired
    ConceptTree conceptTree

    @Autowired
    SecureObjectToken secureObjectToken

    @Value("#{jobParameters['TOP_NODE']}")
    ConceptPath topNode

    @Value(Tables.CONCEPT_DIMENSION)
    private SimpleJdbcInsert dimensionInsert

    @Value(Tables.I2B2)
    private SimpleJdbcInsert i2b2Insert

    @Value(Tables.I2B2_SECURE)
    private SimpleJdbcInsert i2b2SecureInsert

    @SuppressWarnings('PrivateFieldCouldBeFinal')
    @Lazy
    private String metadataXml = { generateMetadataXml() }()

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    DatabaseMetaDataService databaseMetaDataService

    boolean recordId

    Collection<ConceptNode> insert(Collection<ConceptNode> newTreeNodes) throws Exception {
        log.debug "New tree nodes are ${newTreeNodes*.path}"
        newTreeNodes.each {
            log.info "Adding tree node ${it.path}"
        }
        // reserve ids for non-shared concepts and store generated codes in-place
        conceptTree.reserveIdsFor(newTreeNodes)
        // save concept dimension entries
        def newConcepts = newTreeNodes
                .findAll { it.conceptPath && !(it.code in conceptTree.savedConceptCodes) }
                .unique { it.conceptPath }
        newConcepts.each {
            log.info "Adding concept ${it.conceptPath} (${it.code})"
        }
        insertConceptDimension(studyId, newConcepts)
        conceptTree.addToSavedConceptCodes(newConcepts*.code)
        // save tree nodes
        insertI2b2(studyId, newTreeNodes)
        conceptTree.addToSavedNodes(newTreeNodes)
        newTreeNodes
    }

    private int[] insertI2b2(String studyId, Collection<ConceptNode> newConcepts, Date now = new Date()) {
        if (!newConcepts) {
            return new int[0]
        }

        log.debug("Inserting ${newConcepts.size()} new concepts (i2b2/i2b2secure)")

        List<Map> i2b2Rows = []
        List<Map> i2b2SecureRows = []

        newConcepts.each {
            String visualAttributes = visualAttributesFor(it)

            Map i2b2Row = [
                    c_hlevel          : it.level,
                    c_fullname        : it.path.toString(),
                    c_basecode        : null,
                    c_name            : it.name,
                    c_synonym_cd      : 'N',
                    c_visualattributes: visualAttributes,
                    c_metadataxml     : metadataXmlFor(it),
                    c_facttablecolumn : 'CONCEPT_CD',
                    c_tablename       : 'CONCEPT_DIMENSION',
                    c_columnname      : 'CONCEPT_PATH',
                    c_columndatatype  : 'T',
                    c_operator        : 'LIKE',
                    c_dimcode         : it.conceptPath?.toString() ?: '',
                    c_tooltip         : it.path.toString(),
                    m_applied_path    : '@',
                    update_date       : now,
                    download_date     : now,
                    import_date       : now,
                    sourcesystem_cd   : conceptTree.isStudyNode(it) ? studyId : null,
            ]

            if (recordId) {
                i2b2Row.record_id = -1
            }

            Map i2b2SecureRow = new HashMap(i2b2Row)
            i2b2SecureRow.put('secure_obj_token', secureObjectToken as String)
            i2b2SecureRow.remove('record_id')

            i2b2Rows.add(i2b2Row)
            i2b2SecureRows.add(i2b2SecureRow)
        }

        int[] counts1 = i2b2Insert.executeBatch(i2b2Rows as Map[])
        DatabaseUtil.checkUpdateCounts(counts1, 'inserting i2b2')
        int[] counts2 = i2b2SecureInsert.executeBatch(i2b2SecureRows as Map[])
        DatabaseUtil.checkUpdateCounts(counts2, 'inserting i2b2_secure')
        counts2
    }

    private int[] insertConceptDimension(String studyId, Collection<ConceptNode> newConcepts, Date now = new Date()) {
        if (!newConcepts) {
            return new int[0]
        }

        log.info("Inserting ${newConcepts.size()} new concepts " +
                "(concept_dimension)")
        Map<String, Object>[] rows = newConcepts.collect {
            [
                    concept_cd     : it.code,
                    concept_path   : it.conceptPath,
                    name_char      : it.conceptName,
                    concept_blob   : it.uri,
                    update_date    : now,
                    download_date  : now,
                    import_date    : now,
                    sourcesystem_cd: studyId,
            ]
        }

        int[] counts = dimensionInsert.executeBatch(rows)
        DatabaseUtil.checkUpdateCounts(counts, 'inserting conceptDimension')
        counts
    }

    private String metadataXmlFor(ConceptNode concept) {
        switch (concept.type) {
            case ConceptType.NUMERICAL:
                return metadataXml
            case ConceptType.HIGH_DIMENSIONAL:
            case ConceptType.CATEGORICAL:
                return null
            default:
                return null
                //throw new IllegalStateException(
                //        "Unexpected concept type: ${concept.type}")
        }
    }

    private String visualAttributesFor(ConceptNode concept) {
        def res = conceptTree.childrenFor(concept).isEmpty() ?
                (concept.type == ConceptType.HIGH_DIMENSIONAL ? 'LAH' : 'LA') :
                'FA'
        if (res == 'FA' && concept.path == topNode) {
            // add the study modifier for the top node
            res = 'FAS'
        }

        res
    }

    static final String generateMetadataXml() {
        Date now = new Date()
        def writer = new StringWriter()
        def printer = new IndentPrinter(writer)
        printer.autoIndent = false //no indentation

        new MarkupBuilder(printer).ValueMetadata {
            Version('3.02')
            CreationDateTime(now.toString())
            Oktousevalues('Y')
            UnitValues {
                NormalUnits('ratio')
            }
        }

        writer.toString()
    }

    @PostConstruct
    void init() {
        recordId = databaseMetaDataService.getColumnDeclaration(
                new ColumnSpecification(schema: 'i2b2metadata', table: 'i2b2', column: 'record_id'))
    }

}
