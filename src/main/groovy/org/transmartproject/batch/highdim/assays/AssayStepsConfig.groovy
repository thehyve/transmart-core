package org.transmartproject.batch.highdim.assays

import org.springframework.batch.core.Step
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.validator.ValidatingItemProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.transmartproject.batch.batchartifacts.PutInBeanWriter
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DeleteByColumnValueWriter
import org.transmartproject.batch.highdim.jobparams.StandardAssayParametersModule
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Assay spring batch steps configuration
 */
@Configuration
@ComponentScan
class AssayStepsConfig implements StepBuildingConfigurationTrait {

    public static final int DELETE_DATA_CHUNK_SIZE = 50
    public static final int WRITE_ASSAY_CHUNK_SIZE = 50

    @Bean
    Step readMappingFile(MappingsFileRowStore assayMappings,
                         MappingsFileRowValidator mappingFileRowValidator,
                         PlatformAndConceptsContextPromoterListener platformContextPromoterListener) {
        def reader = tsvFileReader(
                mappingFileResource(),
                beanClass: MappingFileRow,
                columnNames: ['studyId', 'siteId', 'subjectId', 'sampleCd',
                              'platform', 'tissueType', 'attr1', 'attr2',
                              'categoryCd', 'source_cd'],
                linesToSkip: 1,
                saveState: false,)


        steps.get('readMappingFile')
                .allowStartIfComplete(true)
                .chunk(1)
                .reader(reader)
                .processor(new ValidatingItemProcessor(adaptValidator(mappingFileRowValidator)))
                .writer(new PutInBeanWriter(bean: assayMappings))
                .stream(mappingFileRowValidator)
                .listener(platformContextPromoterListener)
                .build()
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource mappingFileResource() {
        new JobParameterFileResource(
                parameter: StandardAssayParametersModule.MAP_FILENAME)
    }

    @Bean
    Step deleteCurrentAssays(CurrentAssayIdsReader currentAssayIdsReader) {
        steps.get('deleteCurrentAssays')
                .chunk(DELETE_DATA_CHUNK_SIZE)
                .reader(currentAssayIdsReader)
                .writer(deleteCurrentAssaysWriter())
                .build()
    }

    @Bean
    DeleteByColumnValueWriter<Long> deleteCurrentAssaysWriter() {
        new DeleteByColumnValueWriter<Long>(
                table: Tables.SUBJ_SAMPLE_MAP,
                column: 'assay_id',
                entityName: 'assay ids')
    }

    @Bean
    Step insertAssays(
            ItemWriter<Assay> assayWriter,
            ItemStreamReader<Assay> assayFromMappingFileRowReader,
            SaveAssayIdListener saveAssayIdListener) {
        steps.get('writeAssays')
                .chunk(WRITE_ASSAY_CHUNK_SIZE)
                .reader(assayFromMappingFileRowReader)
                .writer(assayWriter)
                .listener(saveAssayIdListener)
                .build()
    }
}
