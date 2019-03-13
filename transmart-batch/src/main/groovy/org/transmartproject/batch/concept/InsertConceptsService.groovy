package org.transmartproject.batch.concept

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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

    @Value(Tables.TABLE_ACCESS)
    private SimpleJdbcInsert tableAccessInsert

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate

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
            log.debug "Adding tree node ${it.path}"
        }
        // reserve ids for non-shared concepts and store generated codes in-place
        conceptTree.reserveIdsFor(newTreeNodes)
        // save concept dimension entries
        def newConcepts = newTreeNodes
                .findAll { it.conceptPath && !(it.code in conceptTree.savedConceptCodes) }
                .unique { it.conceptPath }
        newConcepts.each {
            log.debug "Adding concept ${it.conceptPath} (${it.code})"
        }
        insertConceptDimension(studyId, newConcepts)
        conceptTree.addToSavedConceptCodes(newConcepts*.code)
        // save tree nodes
        insertI2b2(studyId, newTreeNodes)
        conceptTree.addToSavedNodes(newTreeNodes)
        newTreeNodes
    }

    /**
     * Fetches the full paths of existing table access entries.
     * @return The list of paths.
     */
    private Collection<String> fetchTableAccessEntries() {
        jdbcTemplate.queryForList("SELECT c_fullname FROM $Tables.TABLE_ACCESS", [:], String)
    }

    /**
     * Inserts table access entries for the root nodes (<code>level == 0</code>),
     * if a node with the same path does not already exists.
     *
     * @param rootNodes
     * @return the array with insert counts as documented in
     * {@link org.springframework.jdbc.core.simple.SimpleJdbcInsertOperations#executeBatch}.
     */
    private int[] insertTableAccess(List<ConceptNode> rootNodes) {
        def existingEntries = fetchTableAccessEntries() as Set<String>
        def tableAccessRows = rootNodes.findAll { ConceptNode node ->
            !(node.path.toString() in existingEntries)
        }.collect { ConceptNode node ->
            assert node.level == 0
            log.info "Inserting ${node.name} into $Tables.TABLE_ACCESS"
            [
                c_table_cd: node.name, // transmart needs key the same as first element of path
                c_table_name: (Tables.tableName(Tables.I2B2)).toUpperCase(), // 'I2B2'
                c_protected_access: 'N',
                c_hlevel: 0,
                c_fullname: node.path.toString(),
                c_name: node.name,
                c_synonym_cd: 'N',
                c_visualattributes: 'CAE',
                c_facttablecolumn: 'concept_cd',
                c_dimtablename: 'concept_dimension',
                c_columnname: 'concept_path',
                c_columndatatype: 'T',
                c_operator: 'LIKE',
                c_dimcode: node.conceptPath?.toString() ?: '@',
            ]
        }
        tableAccessInsert.executeBatch(tableAccessRows as Map[])
    }

    private int[] insertI2b2(String studyId, Collection<ConceptNode> newConcepts, Date now = new Date()) {
        if (!newConcepts) {
            return new int[0]
        }

        log.info("Inserting ${newConcepts.size()} new tree nodes (i2b2/i2b2secure)")

        List<Map> i2b2Rows = []
        List<Map> i2b2SecureRows = []

        newConcepts.each {
            String visualAttributes = visualAttributesFor(it)
            boolean isStudyNode = topNode.path == it.path.path
            /* For compatibility with transmartApp, also intermediate nodes
               are treated as concept nodes, in order to be able to select
               subtrees based on a concept path prefix.
               The c_comment column is only for compatibility with transmartApp
               and should not have a role in security checks in the future.
            */
            Map i2b2Row = [
                    c_hlevel          : it.level,
                    c_fullname        : it.path.toString(),
                    c_basecode        : isStudyNode ? '' : it.code,
                    c_name            : it.name,
                    c_synonym_cd      : 'N',
                    c_visualattributes: visualAttributes,
                    c_metadataxml     : metadataXmlFor(it),
                    c_facttablecolumn : 'CONCEPT_CD',
                    c_tablename       : 'CONCEPT_DIMENSION',
                    c_columnname      : 'CONCEPT_PATH',
                    c_columndatatype  : 'T',
                    c_operator        : 'LIKE',
                    c_dimcode         : it.conceptPath?.toString() ?: it.path.toString(),
                    c_tooltip         : it.path.toString(),
                    m_applied_path    : '@',
                    update_date       : now,
                    download_date     : now,
                    import_date       : now,
                    sourcesystem_cd   : conceptTree.isStudyNode(it) ? studyId : null,
                    c_comment         : conceptTree.isStudyNode(it) ? "trial:${studyId}" : null,
            ]

            if (recordId) {
                i2b2Row.record_id = -1
            }

            Map i2b2SecureRow = new HashMap(i2b2Row)
            i2b2SecureRow.remove('c_comment')
            /* In this case, we use 'EXP:PUBLIC' for public nodes, to make the nodes visible in transmartApp. */
            def sot = (it.ontologyNode || secureObjectToken.public) ? 'EXP:PUBLIC' : secureObjectToken.toString()
            i2b2SecureRow.put('secure_obj_token', sot)
            i2b2SecureRow.remove('record_id')

            i2b2Rows.add(i2b2Row)
            i2b2SecureRows.add(i2b2SecureRow)
        }

        int[] tableAccessCounts = insertTableAccess(newConcepts.findAll { it.level == 0 } as List<ConceptNode>)
        DatabaseUtil.checkUpdateCounts(tableAccessCounts, 'inserting table_access')
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
        if (conceptTree.childrenFor(concept).isEmpty()) {
            // leaf node
            switch (concept.type) {
                case ConceptType.HIGH_DIMENSIONAL:
                    return 'LAH'
                case ConceptType.CATEGORICAL:
                    return 'LAC'
                case ConceptType.CATEGORICAL_OPTION:
                    return 'LAO'
                case ConceptType.TEXT:
                    return 'LAT'
                case ConceptType.NUMERICAL:
                    return 'LAN'
                case ConceptType.DATE:
                    return 'LAD'
                default:
                    return 'LA '
            }
        } else {
            if (concept.path == topNode) {
                // add the study modifier for the top node
                return 'FAS'
            } else if (concept.type == ConceptType.CATEGORICAL) {
                return 'FAC'
            }
            return 'FA '
        }
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
