package org.transmartproject.batch.clinical.variable

import groovy.xml.MarkupBuilder
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.concept.ConceptType
import org.transmartproject.batch.db.DatabaseUtil

/**
 * Inserts concepts (from the ConceptTree) that are new
 */
class InsertConceptsTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['STUDY_ID']}")
    String studyId

    @Autowired
    ConceptTree conceptTree

    @Value(Tables.CONCEPT_DIMENSION)
    private SimpleJdbcInsert dimensionInsert

    @Value(Tables.I2B2)
    private SimpleJdbcInsert i2b2Insert

    @Value(Tables.I2B2_SECURE)
    private SimpleJdbcInsert i2b2SecureInsert

    @SuppressWarnings('PrivateFieldCouldBeFinal')
    @Lazy private String metadataXml = { generateMetadataXml() }()

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        conceptTree.reserveIds()

        //gets all new concepts. notice root is not included (not inserted in concept_dimension)
        List<ConceptNode> newConcepts = conceptTree.newConceptNodes

        Date now = new Date()

        if (newConcepts.size() > 0) {
            Map<String,Object>[] rows = newConcepts.collect {
                [
                    concept_cd: it.code,
                    concept_path: it.path.toString(),
                    name_char: it.name,
                    update_date: now,
                    download_date: now,
                    import_date: now,
                    sourcesystem_cd: studyId,
                ]
            }

            int[] counts = dimensionInsert.executeBatch(rows)
            DatabaseUtil.checkUpdateCounts(counts, 'inserting conceptDimension')
            contribution.incrementWriteCount(newConcepts.size())
        }

        if (newConcepts.size() > 0) {
            String comment = "trial:$studyId"
            List<Map> i2b2Rows = []
            List<Map> i2b2SecureRows = []

            newConcepts.each {
                String visualAttributes = conceptTree.childrenFor(it).isEmpty() ? 'LA' : 'FA' //@todo any extra logic?

                Map i2b2Row = [
                        c_hlevel: it.level,
                        c_fullname: it.path.toString(),
                        c_basecode: it.code,
                        c_name: it.name,
                        c_synonym_cd: 'N',
                        c_visualattributes: visualAttributes,
                        c_metadataxml: metadataXmlFor(it),
                        c_facttablecolumn: 'CONCEPT_CD',
                        c_tablename: 'CONCEPT_DIMENSION',
                        c_columnname: 'CONCEPT_PATH',
                        c_columndatatype: 'T',
                        c_operator: 'LIKE',
                        c_dimcode: it.path.toString(),
                        c_comment: comment,
                        c_tooltip: it.path.toString(),
                        m_applied_path: '@',
                        update_date: now,
                        download_date: now,
                        import_date: now,
                        sourcesystem_cd: conceptTree.isStudyNode(it) ? studyId : null,
                        record_id: it.i2b2RecordId,
                ]

                Map i2b2SecureRow = new HashMap(i2b2Row)
                i2b2SecureRow.put('secure_obj_token', 'EXP:PUBLIC')
                i2b2SecureRow.remove('record_id')

                i2b2Rows.add(i2b2Row)
                i2b2SecureRows.add(i2b2SecureRow)
            }

            int[] counts1 = i2b2Insert.executeBatch(i2b2Rows as Map[])
            DatabaseUtil.checkUpdateCounts(counts1, 'inserting i2b2')
            int[] counts2 = i2b2SecureInsert.executeBatch(i2b2SecureRows as Map[])
            DatabaseUtil.checkUpdateCounts(counts2, 'inserting i2b2_secure')
            contribution.incrementWriteCount(newConcepts.size() * 2)
        }

        RepeatStatus.FINISHED
    }

    private String metadataXmlFor(ConceptNode concept) {
        switch (concept.type) {
            case ConceptType.NUMERICAL:
                return metadataXml
            case ConceptType.CATEGORICAL:
                return null
            default:
                throw new IllegalStateException(
                        "Unexpected concept type: ${concept.type}")
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
}
