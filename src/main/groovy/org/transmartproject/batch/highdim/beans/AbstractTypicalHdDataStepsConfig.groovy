package org.transmartproject.batch.highdim.beans

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.batchartifacts.AbstractSplittingItemReader
import org.transmartproject.batch.batchartifacts.CollectMinimumPositiveValueListener
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.db.DbConfig
import org.transmartproject.batch.highdim.assays.AssayStepsConfig
import org.transmartproject.batch.highdim.assays.CurrentAssayIdsReader
import org.transmartproject.batch.highdim.datastd.*
import org.transmartproject.batch.highdim.jobparams.StandardHighDimDataParametersModule
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Spring context for typical hd data loading steps.
 */
@Configuration
@Import([DbConfig, AssayStepsConfig])
abstract class AbstractTypicalHdDataStepsConfig implements StepBuildingConfigurationTrait {

    static int dataFilePassChunkSize = 10000 // non final for testing

    abstract ItemWriter getDeleteCurrentDataWriter()

    abstract ItemWriter<TripleStandardDataValue> getDataWriter()

    @Bean
    Step firstPass() {
        CollectMinimumPositiveValueListener listener = collectMinimumPositiveValueListener()
        TaskletStep step = steps.get('firstPass')
                .chunk(dataFilePassChunkSize)
                .reader(firstPassDataRowSplitterReader())
                .processor(new NegativeDataPointWarningProcessor())
                .stream(listener)
                .listener(listener)
                .listener(logCountsStepListener())
                .build()

        // visitedProbesValidatingReader() doesn't need to be registered
        // (StandardDataRowSplitterReader calls its ItemStream methods)
        step.streams = [firstPassTsvFileReader()]

        step
    }

    @Bean
    Step deleteHdData(CurrentAssayIdsReader currentAssayIdsReader) {
        steps.get('deleteHdData')
                .chunk(100)
                .reader(currentAssayIdsReader)
                .writer(deleteCurrentDataWriter)
                .build()
    }

    @Bean
    Step secondPass() {
        TaskletStep step = steps.get('secondPass')
                .chunk(dataFilePassChunkSize)
                .reader(secondPassReader())
                .writer(dataWriter)
                .processor(patientInjectionProcessor())
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()

        step.streams = [secondPassDataRowSplitterReader()]
        step
    }

    @Bean
    @JobScopeInterfaced
    ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> compositeOfFilteringProcessors(
            @Value("#{jobParameters['SKIP_UNMAPPED_DATA']}") String skipUnmappedData) {
        def processors = [
                new FilterNegativeValuesItemProcessor(),
                new FilterNaNsItemProcessor(),
        ]
        if (skipUnmappedData == 'Y') {
            processors << filterDataWithoutAssayMappingsItemProcessor()
        }
        compositeOf(*processors)
    }

    @Bean
    @StepScope
    DataFileHeaderValidator dataFileHeaderValidator() {
        new DataFileHeaderValidator()
    }

    @Bean
    @JobScope
    ItemStreamReader firstPassTsvFileReader() {
        tsvFileReader(
                dataFileResource(),
                linesToSkip: 1,
                saveHeader: dataFileHeaderValidator(),
                saveState: true)
    }

    @Bean
    @StepScope
    VisitedAnnotationsReadingValidator visitedProbesValidatingReader() {
        new VisitedAnnotationsReadingValidator(delegate: firstPassTsvFileReader())
    }

    @Bean
    @StepScope
    StandardDataRowSplitterReader firstPassDataRowSplitterReader() {
        new StandardDataRowSplitterReader(
                delegate: visitedProbesValidatingReader(),
                dataPointClass: TripleStandardDataValue)
    }

    @Bean
    @JobScopeInterfaced
    org.springframework.core.io.Resource dataFileResource() {
        new JobParameterFileResource(
                parameter: StandardHighDimDataParametersModule.DATA_FILE)
    }

    @Bean
    @JobScope
    FilterDataWithoutAssayMappingsItemProcessor filterDataWithoutAssayMappingsItemProcessor() {
        new FilterDataWithoutAssayMappingsItemProcessor()
    }

    @Bean
    @StepScope
    PatientInjectionProcessor patientInjectionProcessor() {
        new PatientInjectionProcessor()
    }

    @Bean
    @JobScope
    ItemStreamReader secondPassTsvFileReader() {
        tsvFileReader(
                dataFileResource(),
                saveHeader: true,
                linesToSkip: 1,
                saveState: true)
    }

    @Bean
    @JobScope
    TripleDataValueWrappingReader secondPassReader() {
        new TripleDataValueWrappingReader(delegate: secondPassDataRowSplitterReader())
    }

    @Bean
    @StepScope
    StandardDataRowSplitterReader secondPassDataRowSplitterReader(
            AbstractSplittingItemReader.EarlyItemFilter<TripleStandardDataValue> earlyItemFilter) {
        new StandardDataRowSplitterReader(
                delegate: secondPassTsvFileReader(),
                dataPointClass: TripleStandardDataValue,
                eagerLineListener: perDataRowLog2StatisticsListener(),
                earlyItemFilter: earlyItemFilter)
    }

    @Bean
    @JobScope
    PerDataRowLog2StatisticsListener perDataRowLog2StatisticsListener() {
        new PerDataRowLog2StatisticsListener()
    }

    @Bean
    @JobScope
    AbstractSplittingItemReader.EarlyItemFilter<TripleStandardDataValue> earlyItemFilter(
            ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> compositeOfFilteringProcessors
    ) {
        new AbstractSplittingItemReader.EarlyItemFilter<TripleStandardDataValue>() {
            @Override
            boolean keepItem(TripleStandardDataValue item) {
                compositeOfFilteringProcessors.process(item)
            }
        }
    }

    @Bean
    @JobScope
    CollectMinimumPositiveValueListener collectMinimumPositiveValueListener() {
        new CollectMinimumPositiveValueListener()
    }

}
