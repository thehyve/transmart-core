package org.transmartproject.batch.highdim.beans

import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.transmartproject.batch.batchartifacts.CollectMinimumPositiveValueListener
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.highdim.datastd.*
import org.transmartproject.batch.highdim.jobparams.StandardHighDimDataParametersModule
import org.transmartproject.batch.support.JobParameterFileResource

/**
 * Standard HD configuration implementation that use {@link TripleStandardDataValue} as item
 */
abstract class AbstractStandardHighDimJobConfiguration extends AbstractHighDimJobConfiguration {

    static int dataFilePassChunkSize = 10000 // non final for testing

    @Autowired
    FilterNaNsItemProcessor filterNaNsItemProcessor

    /**************
     * First pass *
     **************/

    @Bean
    Step firstPass() {
        CollectMinimumPositiveValueListener listener = collectMinimumPositiveValueListener()
        TaskletStep step = steps.get('firstPass')
                .chunk(dataFilePassChunkSize)
                .reader(firstPassDataRowSplitterReader())
                .processor(warningNegativeDataPointToNaNProcessor())
                .stream(listener)
                .listener(listener)
                .listener(logCountsStepListener())
                .build()

        // visitedProbesValidatingReader() doesn't need to be registered
        // (StandardDataRowSplitterReader calls its ItemStream methods)
        step.streams = [firstPassTsvFileReader(null)]

        step
    }

    @Bean
    @JobScope
    ItemStreamReader firstPassTsvFileReader(DataFileHeaderValidator dataFileHeaderValidator) {
        tsvFileReader(
                dataFileResource(),
                linesToSkip: 1,
                saveHeader: dataFileHeaderValidator,
                saveState: true)
    }

    @Bean
    @StepScope
    VisitedAnnotationsReadingValidator visitedProbesValidatingReader() {
        new VisitedAnnotationsReadingValidator(delegate: firstPassTsvFileReader(null))
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
    NegativeDataPointWarningProcessor warningNegativeDataPointToNaNProcessor() {
        new NegativeDataPointWarningProcessor()
    }

    /***************
     * Second pass *
     ***************/

    @Bean
    Step secondPass() {
        TaskletStep step = steps.get('secondPass')
                .chunk(dataFilePassChunkSize)
                .reader(secondPassReader())
                .writer(dataPointWriter())
                .processor(compositeOf(
                    standardDataValuePatientInjectionProcessor(),
                    filterNaNsItemProcessor
                ))
                .listener(logCountsStepListener())
                .listener(progressWriteListener())
                .build()

        step.streams = [secondPassDataRowSplitterReader()]
        step
    }

    @Bean
    StandardDataValuePatientInjectionProcessor standardDataValuePatientInjectionProcessor() {
        new StandardDataValuePatientInjectionProcessor()
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
    StandardDataRowSplitterReader secondPassDataRowSplitterReader() {
        new StandardDataRowSplitterReader(
                delegate: secondPassTsvFileReader(),
                dataPointClass: TripleStandardDataValue,
                eagerLineListener: perDataRowLog2StatisticsListener())
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
