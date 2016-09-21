package org.transmartproject.batch.gwas

import com.google.common.io.Files
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.beans.GenericFunctionalTestConfiguration
import org.transmartproject.batch.beans.PersistentContext
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.RowCounter
import org.transmartproject.batch.db.TableTruncator
import org.transmartproject.batch.junit.FileCorruptingTestTrait
import org.transmartproject.batch.support.TableLists

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

/**
 * Corrupt both analysis files and then try to resume.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = GenericFunctionalTestConfiguration)
class GwasMidwayFailTests implements FileCorruptingTestTrait {

    private final static String STUDY_ID = 'MAGIC'

    private static int originalChunkSize

    @Autowired
    RowCounter rowCounter

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    TableTruncator truncator

    List<File> originalDataFiles = [
            new File('studies/MAGIC/gwas/mod_MAGIC_2hrGlucose_AdjustedForBMI.tsv'),
            new File('studies/MAGIC/gwas/mod_MAGIC_FastingGlucose.tsv'),
    ]

    @BeforeClass
    static void beforeClass() {
        originalChunkSize = GwasJobConfiguration.chunkSize
        GwasJobConfiguration.chunkSize = 200
    }

    @AfterClass
    static void cleanDatabase() {
        GwasJobConfiguration.chunkSize = originalChunkSize

        PersistentContext.truncator.
                truncate(TableLists.GWAS_TABLE_LISTS + 'ts_batch.batch_job_instance')
    }

    @Test
    @SuppressWarnings('JUnitTestMethodWithoutAssert')
    void testFailMidwayFirstStep() {
        def corruptedFiles = originalDataFiles.collect {
            File corrupt = corruptFile(it, 585, 2, 'CORRUPTION')
            def renamedCorruptFile = new File(corrupt.parent, it.name)
            corrupt.renameTo(renamedCorruptFile)
            renamedCorruptFile
        }

        def params = [
                '-p', 'studies/' + STUDY_ID + '/gwas.params',
                '-d', 'DATA_LOCATION=' + corruptedFiles.first().parent]

        // first execution
        firstExecution(params)

        // fix the file
        corruptedFiles.eachWithIndex { File corrupted, i ->
            Files.copy(originalDataFiles[i], corrupted)
        }
        secondExecution(params)

        def count = rowCounter.count Tables.BIO_ASSAY_ANALYSIS_GWAS

        assertThat count, is(equalTo(
                GwasCleanScenarioTests.NUMBER_OF_SNP_FASTING_GLUCOSE +
                        GwasCleanScenarioTests.NUMBER_OF_SNPS_2HR_GLUCOSE))
    }
}
