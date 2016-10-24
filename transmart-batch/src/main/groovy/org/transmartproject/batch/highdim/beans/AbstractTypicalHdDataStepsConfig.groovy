package org.transmartproject.batch.highdim.beans

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.transmartproject.batch.batchartifacts.CollectMinimumPositiveValueListener
import org.transmartproject.batch.batchartifacts.HeaderParsingLineCallbackHandler
import org.transmartproject.batch.batchartifacts.HeaderSavingLineCallbackHandler
import org.transmartproject.batch.batchartifacts.MultipleItemsLineItemReader
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.beans.StepScopeInterfaced
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
    Step firstPass(ItemStreamReader firstPassReader,
                   ItemProcessor compositeOfFirstPassItemProcessors) {
        CollectMinimumPositiveValueListener listener = collectMinimumPositiveValueListener()

        TaskletStep step = steps.get('firstPass')
                .chunk(dataFilePassChunkSize)
                .reader(firstPassReader)
                .processor(compositeOfFirstPassItemProcessors)
                .stream(listener)
                .listener(listener)
                .listener(logCountsStepListener())
                .build()

        step
    }

    @Bean
    @JobScopeInterfaced
    ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> compositeOfFirstPassItemProcessors(
            @Value("#{jobParameters['DATA_TYPE']}") String dataType) {
        def processors = []
        if (dataType == 'L') {
            processors << calculateRawValueFromTheLogItemProcessor()
        } else if (dataType != 'R') {
            throw new IllegalArgumentException("Unsupported DATA_TYPE=${dataType}.")
        }
        processors << new NegativeDataPointWarningProcessor()

        compositeOf(*processors)
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
    Step secondPass(ItemStreamReader secondPassMultipleItemsLineItemReader) {
        TaskletStep step = steps.get('secondPass')
                .chunk(dataFilePassChunkSize)
                .reader(secondPassMultipleItemsLineItemReader)
                .writer(dataWriter)
                .processor(patientInjectionProcessor())
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()

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
        processors << tripleStandardDataValueLogCalculationProcessor()
        compositeOf(*processors)
    }

    @Bean
    @JobScope
    TripleStandardDataValueLogCalculationProcessor tripleStandardDataValueLogCalculationProcessor() {
        new TripleStandardDataValueLogCalculationProcessor()
    }

    @Bean
    @JobScope
    CalculateRawValueFromTheLogItemProcessor calculateRawValueFromTheLogItemProcessor() {
        new CalculateRawValueFromTheLogItemProcessor()
    }

    @Bean
    @StepScope
    VisitedAnnotationsReadingValidator visitedProbesValidatingReader(
            AbstractItemCountingItemStreamItemReader<FieldSet> tsvFileReader) {
        new VisitedAnnotationsReadingValidator(delegate: tsvFileReader)
    }

    @Bean
    @StepScopeInterfaced
    ItemStreamReader firstPassReader(
            VisitedAnnotationsReadingValidator visitedProbesValidatingReader,
            StandardMultipleVariablesPerSampleFieldSetMapper standardMultipleVariablesPerSampleFieldSetMapper
    ) {
        MultipleItemsLineItemReader reader = new MultipleItemsLineItemReader(
                multipleItemsFieldSetMapper: standardMultipleVariablesPerSampleFieldSetMapper,
                itemStreamReader: visitedProbesValidatingReader,
        )

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
    @StepScope
    HeaderSavingLineCallbackHandler headerSavingLineCallbackHandler() {
        new HeaderSavingLineCallbackHandler()
    }

    @Bean
    @StepScope
    HeaderParsingLineCallbackHandler headerParsingLineCallbackHandler(
            StandardMultipleVariablesPerSampleFieldSetMapper standardMultipleVariablesPerSampleFieldSetMapper) {
        new HeaderParsingLineCallbackHandler(
                registeredSuffixes: standardMultipleVariablesPerSampleFieldSetMapper.fieldSetters.keySet(),
                defaultSuffix: 'val'
        )
    }

    @Bean
    @StepScope
    StandardMultipleVariablesPerSampleFieldSetMapper standardMultipleVariablesPerSampleFieldSetMapper() {
        new StandardMultipleVariablesPerSampleFieldSetMapper()
    }

    @Bean
    @StepScope
    AbstractItemCountingItemStreamItemReader<FieldSet> tsvFileReader(
            org.springframework.core.io.Resource dataFileResource,
            HeaderParsingLineCallbackHandler headerParsingLineCallbackHandler) {
        tsvFileReader(
                dataFileResource,
                saveHeader: headerParsingLineCallbackHandler,
                columnNames: 'auto',
                linesToSkip: 1,
                saveState: true)
    }

    @Bean
    @StepScopeInterfaced
    ItemStreamReader secondPassMultipleItemsLineItemReader(
            AbstractItemCountingItemStreamItemReader<FieldSet> tsvFileReader,
            StandardMultipleVariablesPerSampleFieldSetMapper standardMultipleVariablesPerSampleFieldSetMapper,
            TripleStandardDataValueRowItemsProcessor tripleStandardDataValueRowItemsProcessor) {
        new MultipleItemsLineItemReader(
                multipleItemsFieldSetMapper: standardMultipleVariablesPerSampleFieldSetMapper,
                itemStreamReader: tsvFileReader,
                rowItemsProcessor: tripleStandardDataValueRowItemsProcessor
        )
    }

    @Bean
    @StepScope
    TripleStandardDataValueRowItemsProcessor tripleStandardDataValueRowItemsProcessor(
            ItemProcessor<TripleStandardDataValue, TripleStandardDataValue> compositeOfEarlyItemProcessors) {
        new TripleStandardDataValueRowItemsProcessor(
                itemsPreProcessor: compositeOfEarlyItemProcessors
        )
    }

    @Bean
    @JobScope
    CollectMinimumPositiveValueListener collectMinimumPositiveValueListener() {
        new CollectMinimumPositiveValueListener()
    }

}
