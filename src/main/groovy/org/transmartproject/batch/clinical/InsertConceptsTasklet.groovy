package org.transmartproject.batch.clinical

import groovy.xml.MarkupBuilder
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.model.ConceptNode
import org.transmartproject.batch.model.ConceptTree
import org.transmartproject.batch.model.VariableType
import org.transmartproject.batch.support.DatabaseObject

import javax.annotation.PostConstruct

/**
 * Inserts concepts (from the ConceptTree) that are new
 */
class InsertConceptsTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['studyId']}")
    String studyId

    @Value("#{clinicalJobContext.conceptTree}")
    ConceptTree conceptTree

    private SimpleJdbcInsert dimensionInsert
    private SimpleJdbcInsert i2b2Insert
    private SimpleJdbcInsert i2b2SecureInsert

    @Lazy private String metadataXml = { createMetadataXml() }()

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        //gets all new concepts. notice root is not included (not inserted in concept_dimension)
        List<ConceptNode> newConcepts = conceptTree.root.allChildren.findAll { it.isNew }

        Date now = new Date()

        if (newConcepts.size() > 0) {
            Map<String,Object>[] rows = newConcepts.collect {
                [
                    concept_cd: it.code,
                    concept_path: it.path,
                    name_char: it.name,
                    update_date: now,
                    download_date: now,
                    import_date: now,
                    sourcesystem_cd: studyId,
                ]
            }

            int[] counts = dimensionInsert.executeBatch(rows)
            DatabaseObject.checkUpdateCounts(counts, 'inserting conceptDimension')
            contribution.incrementWriteCount(newConcepts.size())
        }

        if (conceptTree.root.isNew) {
            //adds root to list if new
            newConcepts.add(conceptTree.root)
        }

        if (newConcepts.size() > 0) {
            String comment = "trial:$studyId"
            List<Map> i2b2Rows = []
            List<Map> i2b2SecureRows = []

            newConcepts.each {
                String visualAttributes = it.children.isEmpty() ? 'LA' : 'FA' //@todo any extra logic?

                Map i2b2Row = [
                        c_hlevel: it.level,
                        c_fullname: it.path,
                        c_basecode: it.code,
                        c_name: it.name,
                        c_synonym_cd: 'N', //@todo any extra logic?
                        c_visualattributes: visualAttributes,
                        c_metadataxml: metadataXmlFor(it),
                        c_facttablecolumn: 'CONCEPT_CD',
                        c_tablename: 'CONCEPT_DIMENSION',
                        c_columnname: 'CONCEPT_PATH',
                        c_columndatatype: 'T',
                        c_operator: 'LIKE',
                        c_dimcode: it.path,
                        c_comment: comment,
                        c_tooltip: it.path,
                        m_applied_path: '@',
                        update_date: now,
                        download_date: now,
                        import_date: now,
                        sourcesystem_cd: (it == conceptTree.root ? null : studyId),
                        record_id: it.i2b2RecordId,
                ]

                Map i2b2SecureRow = new HashMap(i2b2Row)
                i2b2SecureRow.put('secure_obj_token', 'EXP:PUBLIC')
                i2b2SecureRow.remove('record_id')

                i2b2Rows.add(i2b2Row)
                i2b2SecureRows.add(i2b2SecureRow)
            }

            int[] counts1 = i2b2Insert.executeBatch(i2b2Rows as Map[])
            DatabaseObject.checkUpdateCounts(counts1, 'inserting i2b2')
            int[] counts2 = i2b2SecureInsert.executeBatch(i2b2SecureRows as Map[])
            DatabaseObject.checkUpdateCounts(counts2, 'inserting i2b2_secure')
            contribution.incrementWriteCount(newConcepts.size() * 2)
        }

        println contribution

        return RepeatStatus.FINISHED
    }

    private String metadataXmlFor(ConceptNode concept) {
        if (VariableType.NUMERICAL == concept.type) {
            return metadataXml
        }
        null
    }

    static final String createMetadataXml() {
        Date now = new Date()
        def writer = new StringWriter()
        def printer = new IndentPrinter(writer)
        printer.autoIndent = false //no indentation

        def xml = new MarkupBuilder(printer).ValueMetadata {
            Version('3.02')
            CreationDateTime(now.toString())
            Oktousevalues('Y')
            UnitValues {
                NormalUnits ('ratio')
            }
        }

        writer.toString()
    }


    @PostConstruct
    void initInserts() {
        dimensionInsert = new SimpleJdbcInsert(jdbcTemplate)
        dimensionInsert.withSchemaName(DatabaseObject.Schema.I2B2DEMODATA).withTableName('concept_dimension')

        i2b2Insert = new SimpleJdbcInsert(jdbcTemplate)
        i2b2Insert.withSchemaName(DatabaseObject.Schema.I2B2METADATA).withTableName('i2b2')

        i2b2SecureInsert = new SimpleJdbcInsert(jdbcTemplate)
        i2b2SecureInsert.withSchemaName(DatabaseObject.Schema.I2B2METADATA).withTableName('i2b2_secure')
    }

}
