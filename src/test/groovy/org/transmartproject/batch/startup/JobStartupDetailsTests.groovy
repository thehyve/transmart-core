package org.transmartproject.batch.startup

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.transmartproject.batch.clinical.ClinicalDataLoadJobConfiguration
import org.transmartproject.batch.highdim.cnv.data.CnvDataJobConfig
import org.transmartproject.batch.highdim.mrna.data.MrnaDataJobConfig
import org.transmartproject.batch.highdim.mrna.platform.MrnaPlatformJobConfig

import java.nio.file.Path

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests {@link org.transmartproject.batch.startup.JobStartupDetails}.
 */
class JobStartupDetailsTests {

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    @Test
    void testSeparateStudyParamsFile() {
        def dataFileName = 'empty_data_file.tsv'
        def clinicalDataFolderName = 'clinical'

        createTmpFile 'study.params', """
            STUDY_ID=test_study_id
            TOP_NODE=\\A\\B\\C
            SECURITY_REQUIRED=Y
        """
        createTmpFile dataFileName, '', clinicalDataFolderName
        Path paramsFile = createTmpFile 'clinical.params', """
            COLUMN_MAP_FILE=${dataFileName}
        """, clinicalDataFolderName

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)

        assertThat startupDetails['STUDY_ID'], equalTo('TEST_STUDY_ID')
        assertThat startupDetails['TOP_NODE'], equalTo('\\A\\B\\C')
        assertThat startupDetails['SECURITY_REQUIRED'], equalTo('Y')
    }

    @Test
    void testClinicalJobStartupDetails() {
        def dataFileName = 'empty_data_file.tsv'
        def wordMapFileName = 'empty_word_map_file.tsv'
        def recordExclFileName = 'empty_rcd_excl_file.tsv'
        def xtrialFileName = 'empty_xtrial_file.tsv'
        def tagsFileName = 'empty_tags_file.tsv'

        createTmpFile dataFileName
        createTmpFile wordMapFileName
        createTmpFile recordExclFileName
        createTmpFile xtrialFileName
        createTmpFile tagsFileName
        Path paramsFile = createTmpFile 'clinical.params', """
            STUDY_ID=test
            COLUMN_MAP_FILE=${dataFileName}
            WORD_MAP_FILE=${wordMapFileName}
            RECORD_EXCLUSION_FILE=${recordExclFileName}
            XTRIAL_FILE=${xtrialFileName}
            TAGS_FILE=${tagsFileName}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)

        assertThat startupDetails, hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration))
        assertThat startupDetails['COLUMN_MAP_FILE'], notNullValue()
        assert startupDetails['COLUMN_MAP_FILE'].endsWith(dataFileName)
        assertThat startupDetails['WORD_MAP_FILE'], notNullValue()
        assert startupDetails['WORD_MAP_FILE'].endsWith(wordMapFileName)
        assertThat startupDetails['RECORD_EXCLUSION_FILE'], notNullValue()
        assert startupDetails['RECORD_EXCLUSION_FILE'].endsWith(recordExclFileName)
        assertThat startupDetails['XTRIAL_FILE'], notNullValue()
        assert startupDetails['XTRIAL_FILE'].endsWith(xtrialFileName)
        assertThat startupDetails['TAGS_FILE'], notNullValue()
        assert startupDetails['TAGS_FILE'].endsWith(tagsFileName)
    }

    @Test
    void testOldDataFileLocationIsSupported() {
        def dataFileName = 'empty_data_file.tsv'

        createTmpFile dataFileName, '', 'clinical'
        Path paramsFile = createTmpFile 'clinical.params', """
            STUDY_ID=test
            COLUMN_MAP_FILE=${dataFileName}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)

        assertThat startupDetails, hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration))
        assertThat startupDetails['COLUMN_MAP_FILE'], notNullValue()
        assert startupDetails['COLUMN_MAP_FILE'].endsWith(dataFileName)
    }

    @Test(expected = InvalidParametersFileException)
    void testStudyIdIsRequired() {
        def dataFileName = 'empty_data_file.tsv'

        createTmpFile dataFileName
        Path paramsFile = createTmpFile 'clinical.params', """
            COLUMN_MAP_FILE=${dataFileName}
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test(expected = InvalidParametersFileException)
    void testNoOverridingAllowed() {
        def dataFileName = 'empty_data_file.tsv'

        createTmpFile 'study.params', """
            STUDY_ID=test_study_id
        """

        createTmpFile dataFileName
        Path paramsFile = createTmpFile 'clinical.params', """
            STUDY_ID=test_study_id
            COLUMN_MAP_FILE=${dataFileName}
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test(expected = InvalidParametersFileException)
    void testGiveExceptionOnUnknownParameters() {
        def dataFileName = 'empty_data_file.tsv'

        createTmpFile dataFileName
        Path paramsFile = createTmpFile 'clinical.params', """
            TO_CAUSE_EXCEPTION=test
            STUDY_ID=test_study_id
            COLUMN_MAP_FILE=${dataFileName}
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test
    void testCommentsSupported() {
        def dataFileName = 'empty_data_file.tsv'

        createTmpFile dataFileName
        Path paramsFile = createTmpFile 'clinical.params', """
            #TO_CAUSE_EXCEPTION=test
            STUDY_ID=test_study_id
            COLUMN_MAP_FILE=${dataFileName}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)
        assertThat startupDetails, hasProperty('jobPath', equalTo(ClinicalDataLoadJobConfiguration))
        assertThat startupDetails['TO_CAUSE_EXCEPTION'], nullValue()
    }

    @Test(expected = InvalidParametersFileException)
    void testExceptionOnDataFileDoesNotExist() {
        Path paramsFile = createTmpFile 'clinical.params', """
            STUDY_ID=test_study_id
            COLUMN_MAP_FILE=nonexisting_file.tsv
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test(expected = InvalidParametersFileException)
    void testColumnMappingFileIsRequired() {
        Path paramsFile = createTmpFile 'clinical.params', """
            STUDY_ID=test_study_id
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test
    void testPlatformJobStartupDetails() {
        def annotationDataFileName = 'empty_annotation_file.tsv'
        createTmpFile annotationDataFileName
        Path paramsFile = createTmpFile 'mrna_annotation.params', """
            PLATFORM=test_platform_id
            TITLE=test_title
            ORGANISM=test_organism
            ANNOTATIONS_FILE=${annotationDataFileName}
            GENOME_RELEASE=hg18
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)
        assertThat startupDetails, hasProperty('jobPath', equalTo(MrnaPlatformJobConfig))
        assertThat startupDetails['ANNOTATIONS_FILE'], notNullValue()
        assert startupDetails['ANNOTATIONS_FILE'].endsWith(annotationDataFileName)
        assertThat startupDetails['PLATFORM'], equalTo('TEST_PLATFORM_ID')
        assertThat startupDetails['TITLE'], equalTo('test_title')
        assertThat startupDetails['ORGANISM'], equalTo('test_organism')
        assertThat startupDetails['GENOME_RELEASE'], equalTo('hg18')
    }

    @Test
    void testPlatformJobStartupDetailsDefaults() {
        def annotationDataFileName = 'empty_annotation_file.tsv'
        createTmpFile annotationDataFileName
        Path paramsFile = createTmpFile 'mrna_annotation.params', """
            PLATFORM=test_platform_id
            TITLE=test_title
            ANNOTATIONS_FILE=${annotationDataFileName}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)

        assertThat startupDetails['ORGANISM'], equalTo('Homo Sapiens')
        assertThat startupDetails['MARKER_TYPE'], equalTo('Gene Expression')
        assertThat startupDetails['GENOME_RELEASE'], nullValue()
    }

    @Test(expected = InvalidParametersFileException)
    void testPlatformIdIsRequired() {
        def annotationDataFileName = 'empty_annotation_file.tsv'
        createTmpFile annotationDataFileName
        Path paramsFile = createTmpFile 'mrna_annotation.params', """
            TITLE=test_title
            ANNOTATIONS_FILE=${annotationDataFileName}
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test(expected = InvalidParametersFileException)
    void testPlatformTitleIdIsRequired() {
        def annotationDataFileName = 'empty_annotation_file.tsv'
        createTmpFile annotationDataFileName
        Path paramsFile = createTmpFile 'mrna_annotation.params', """
            PLATFORM=test_platform_id
            ANNOTATIONS_FILE=${annotationDataFileName}
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test(expected = InvalidParametersFileException)
    void testAnnoationFileIsRequired() {
        Path paramsFile = createTmpFile 'mrna_annotation.params', """
            PLATFORM=test_platform_id
            TITLE=test_title
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test
    void testHDDataJobStartupDetails() {
        def dataFileName = 'empty_data_file.tsv'
        def ssmFileName = 'empty_ssm_file.tsv'
        createTmpFile dataFileName
        createTmpFile ssmFileName
        Path paramsFile = createTmpFile 'expression.params', """
            STUDY_ID=test_study
            DATA_FILE=${dataFileName}
            MAP_FILENAME=${ssmFileName}
            DATA_TYPE=R
            LOG_BASE=2
            ALLOW_MISSING_ANNOTATIONS=Y
            ZERO_MEANS_NO_INFO=Y
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)

        assertThat startupDetails, hasProperty('jobPath', equalTo(MrnaDataJobConfig))
        assertThat startupDetails['DATA_FILE'], notNullValue()
        assert startupDetails['DATA_FILE'].endsWith(dataFileName)
        assertThat startupDetails['MAP_FILENAME'], notNullValue()
        assert startupDetails['MAP_FILENAME'].endsWith(ssmFileName)
        assertThat startupDetails['DATA_TYPE'], equalTo('R')
        assertThat startupDetails['LOG_BASE'], equalTo('2')
        assertThat startupDetails['ALLOW_MISSING_ANNOTATIONS'], equalTo('Y')
        assertThat startupDetails['ZERO_MEANS_NO_INFO'], equalTo('Y')
    }

    @Test
    void testHDDataJobStartupDetailsDefaults() {
        def dataFileName = 'empty_data_file.tsv'
        def ssmFileName = 'empty_ssm_file.tsv'
        createTmpFile dataFileName
        createTmpFile ssmFileName
        Path paramsFile = createTmpFile 'expression.params', """
            STUDY_ID=test_study
            DATA_FILE=${dataFileName}
            MAP_FILENAME=${ssmFileName}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)

        assertThat startupDetails['DATA_TYPE'], equalTo('R')
        assertThat startupDetails['LOG_BASE'], equalTo('2')
        assertThat startupDetails['ALLOW_MISSING_ANNOTATIONS'], equalTo('N')
        assertThat startupDetails['ZERO_MEANS_NO_INFO'], equalTo('N')
    }

    @Test
    void testCnvProbIsNotOneDefaultIsError() {
        def dataFileName = 'empty_data_file.tsv'
        def ssmFileName = 'empty_ssm_file.tsv'

        createTmpFile dataFileName
        createTmpFile ssmFileName
        Path paramsFile = createTmpFile 'cnv.params', """
            STUDY_ID=test_study
            DATA_FILE=${dataFileName}
            MAP_FILENAME=${ssmFileName}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)
        assertThat startupDetails, hasProperty('jobPath', equalTo(CnvDataJobConfig))
        assertThat startupDetails['PROB_IS_NOT_1'], equalTo('ERROR')
    }

    @Test
    void testCnvProbIsNotOne() {
        def dataFileName = 'empty_data_file.tsv'
        def ssmFileName = 'empty_ssm_file.tsv'
        def providedArgument = 'WARN'

        createTmpFile dataFileName
        createTmpFile ssmFileName
        Path paramsFile = createTmpFile 'cnv.params', """
            STUDY_ID=test_study
            DATA_FILE=${dataFileName}
            MAP_FILENAME=${ssmFileName}
            PROB_IS_NOT_1=${providedArgument}
        """

        JobStartupDetails startupDetails = JobStartupDetails.fromFile(paramsFile)
        assertThat startupDetails['PROB_IS_NOT_1'], equalTo(providedArgument)
    }

    @Test(expected = InvalidParametersFileException)
    void testCnvProbIsNotOneExpectedError() {
        def dataFileName = 'empty_data_file.tsv'
        def ssmFileName = 'empty_ssm_file.tsv'
        def providedArgument = 'FAIL!'

        createTmpFile dataFileName
        createTmpFile ssmFileName
        Path paramsFile = createTmpFile 'cnv.params', """
            STUDY_ID=test_study
            DATA_FILE=${dataFileName}
            MAP_FILENAME=${ssmFileName}
            PROB_IS_NOT_1=${providedArgument}
        """

        JobStartupDetails.fromFile(paramsFile)
    }

    @Test
    void testThrowExceptionOnEmptyValue() {
        exception.expect(InvalidParametersFileException)
        exception.expectMessage(
                allOf(
                        startsWith('Following parameters are specified without a value:'),
                        containsString('TOP_NODE'),
                        containsString('XTRIAL_FILE'),
                        endsWith('Please provide a value or remove parameter.')
                ))

        def dataFileName = 'empty_data_file.tsv'
        def clinicalDataFolderName = 'clinical'

        createTmpFile 'study.params', """
            STUDY_ID=test_study_id
            TOP_NODE=
        """
        createTmpFile dataFileName, '', clinicalDataFolderName
        Path paramsFile = createTmpFile 'clinical.params', """
            COLUMN_MAP_FILE=${dataFileName}
            XTRIAL_FILE=
        """, clinicalDataFolderName

        JobStartupDetails.fromFile(paramsFile)
    }

    Path createTmpFile(String fileName, String content = '', String folder = '') {
        File tmpFile
        if (folder) {
            File folderFile = new File(temporaryFolder.root, folder)
            folderFile.mkdir()
            tmpFile = new File(folderFile, fileName)
        } else {
            tmpFile = new File(temporaryFolder.root, fileName)
        }
        tmpFile.text = content
        tmpFile.toPath()
    }

}
