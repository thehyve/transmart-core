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
    ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> compositeOfEarlyItemProcessors(
            @Value("#{jobParameters['SKIP_UNMAPPED_DATA']}") String skipUnmappedData,
            @Value("#{jobParameters['ZERO_MEANS_NO_INFO']}") String zeroMeansNoInfo,
            @Value("#{jobParameters['DATA_TYPE']}") String dataType) {
        def processors = [
                new FilterNaNsItemProcessor(),
        ]
        if (skipUnmappedData == 'Y') {
            processors << filterDataWithoutAssayMappingsItemProcessor()
        }
        if (dataType == 'L') {
            processors << calculateRawValueFromTheLogItemProcessor()
        } else if (dataType == 'R') {
            processors << new FilterNegativeValuesItemProcessor()
            if (zeroMeansNoInfo == 'Y') {
                processors << new FilterZerosItemProcessor()
            }
        } else {
            throw new IllegalArgumentException("Unsupported DATA_TYPE=${dataType}.")
        }
        compositeOf(*processors)
    }

    @Bean
    @JobScope
    CalculateRawValueFromTheLogItemProcessor calculateRawValueFromTheLogItemProcessor() {
        new CalculateRawValueFromTheLogItemProcessor()
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
    StandardDataRowSplitterReader firstPassDataRowSplitterReader(
            @Value("#{jobParameters['DATA_TYPE']}") String dataType
    ) {
        StandardDataRowSplitterReader reader = new StandardDataRowSplitterReader(
                delegate: visitedProbesValidatingReader(),
                dataPointClass: TripleStandardDataValue)

        if (dataType == 'L') {
            reader.earlyItemProcessor = calculateRawValueFromTheLogItemProcessor()
        } else if (dataType != 'R') {
            throw new IllegalArgumentException("Unsupported DATA_TYPE=${dataType}.")
        }

        reader
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
            ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> compositeOfEarlyItemProcessors) {
        new StandardDataRowSplitterReader(
                delegate: secondPassTsvFileReader(),
                dataPointClass: TripleStandardDataValue,
                eagerLineListener: perDataRowLog2StatisticsListener(),
                earlyItemProcessor: compositeOfEarlyItemProcessors)
    }

    @Bean
    @JobScope
    PerDataRowLog2StatisticsListener perDataRowLog2StatisticsListener() {
        new PerDataRowLog2StatisticsListener()
    }

    @Bean
    @JobScope
    CollectMinimumPositiveValueListener collectMinimumPositiveValueListener() {
        new CollectMinimumPositiveValueListener()
    }

}
