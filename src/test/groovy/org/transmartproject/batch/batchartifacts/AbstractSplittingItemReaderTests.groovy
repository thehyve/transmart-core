package org.transmartproject.batch.batchartifacts

import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.batch.core.*
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.ItemStreamReader
import org.springframework.batch.item.file.transform.DefaultFieldSet
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.transmartproject.batch.highdim.datastd.OnlineMeanAndVarianceCalculator

import javax.sql.DataSource

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test the {@link AbstractSplittingItemReader} class.
 */
@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = AbstractSplittingItemReaderTestsConfiguration)
@SuppressWarnings('JUnitTestMethodWithoutAssert')
class AbstractSplittingItemReaderTests {

    @EnableBatchProcessing
    @Configuration
    static class AbstractSplittingItemReaderTestsConfiguration {

        @Autowired
        JobBuilderFactory jobs

        @Autowired
        StepBuilderFactory steps

        @Bean
        DataSource dataSource() {
            // we need a pool because we're using an in-memory database
            // it would otherwise go away prematurely
            new JdbcConnectionPool(
                    new JdbcDataSource(
                            url: 'jdbc:h2:mem:',
                            user: 'sa',
                            password: '',))
        }

        @Bean
        Job job() {
            jobs.get('job')
                    .start(step())
                    .build()
        }

        @Bean
        Step step() {
            steps.get('step')
                    .chunk(2)
                    .reader(fieldSetSplittingItemReader())
                    .writer(putInBeanWriter())
                    .build()
        }

        private ItemStreamReader<FieldSet> innerReader() {
            new IterableItemReader<FieldSet>(
                    iterable: [
                            new DefaultFieldSet([1, 2, 3, 4] as String[]), // mean 2.5
                            new DefaultFieldSet([5, 6, 7, 8] as String[]), // mean 6.5
                            new DefaultFieldSet([9] as String[]),          // mean 9
                    ],
                    name: 'iterableItemReader',
            )
        }

        @Bean
        FieldSetSplittingItemReader fieldSetSplittingItemReader() {
            new FieldSetSplittingItemReader(delegate: innerReader())
        }

        @Bean
        PutInBeanWriter putInBeanWriter() {
            new PutInBeanWriter(/* do not configure it yet */)
        }

        @Bean
        MeanListener meanListener() {
            new MeanListener()
        }

        @Bean
        @Scope('prototype')
        FailOnNItemSink<Double> failOnNItemSink() {
            // ATTENTION: we can only do the wrapping at the writer level
            // because the chunk size aligns with the size of the inner reader
            // field sets (chunk size is 2 and the field sets have size 4)
            new FailOnNItemSink() {
                void leftShift(Double o) {
                    super.leftShift(o / meanListener().mean)
                }
            }
        }
    }

    static class FieldSetSplittingItemReader extends AbstractSplittingItemReader<Integer> {
        @Override
        protected Integer doRead() {
            currentFieldSet.readInt(position)
        }
    }

    static class FailOnNItemSink<T> {
        int n /* fail on (n + 1)-th item */
        List<T> items = []

        void leftShift(T o) {
            if (items.size() == n) {
                throw new IllegalStateException(
                        "Can't take more items (current $items)")
            }

            items << o
        }
    }

    static class MeanListener implements AbstractSplittingItemReader.EagerLineListener {
        public static final String KEY = 'meanListener'
        private double mean

        @Override
        void onLine(FieldSet fieldSet, Collection keptItems) {
            OnlineMeanAndVarianceCalculator c =
                    new OnlineMeanAndVarianceCalculator()
            keptItems.each { c.push it }
            mean = c.mean
        }

        @Override
        void open(ExecutionContext executionContext) throws ItemStreamException {
            if (executionContext.containsKey(KEY)) {
                mean = executionContext.getDouble(KEY)
            }
        }

        @Override
        void update(ExecutionContext executionContext) throws ItemStreamException {
            executionContext.putDouble(KEY, mean)
        }

        @Override
        @SuppressWarnings('CloseWithoutCloseable')
        void close() throws ItemStreamException {
            mean = 0d
        }
    }

    @Autowired
    private PutInBeanWriter putInBeanWriter

    @Autowired
    private JobLauncher jobLauncher

    @Autowired
    private Job job

    @Autowired
    DataSource dataSource

    @Autowired
    private FieldSetSplittingItemReader fieldSetSplittingItemReader

    @Autowired
    private MeanListener meanListener

    @Autowired
    private FailOnNItemSink<Double> meanScaledItemSink

    @Before
    void before() {
        def populator = new ResourceDatabasePopulator()
        populator.addScript(
                new ClassPathResource('org/springframework/batch/core/schema-h2.sql'))
        DatabasePopulatorUtils.execute populator, dataSource
    }

    @After
    void after() {
        def populator = new ResourceDatabasePopulator()
        populator.addScript(
                new ClassPathResource('org/springframework/batch/core/schema-drop-h2.sql'))
        DatabasePopulatorUtils.execute populator, dataSource
    }

    private doFailingRun(List expected) {
        JobExecution exec = jobLauncher.run(job, new JobParameters())

        assertThat exec.exitStatus, allOf(
                hasProperty('exitCode', is(ExitStatus.FAILED.exitCode)),
                hasProperty('exitDescription',
                        containsString("Can't take more items")))
        assertThat putInBeanWriter.bean,
                hasProperty('items', contains(expected.collect { is it }))
    }

    private doSuccessfulRun(List expected) {
        JobExecution exec = jobLauncher.run(job, new JobParameters())

        assertThat exec.exitStatus.exitCode, is(ExitStatus.COMPLETED.exitCode)
        // 5 is part of a chunk that failed before, so it shows up again
        assertThat putInBeanWriter.bean,
                hasProperty('items',
                        contains(expected.collect { is it }))
    }

    @Test
    void resumesCorrectlyWithoutCacheInitialFetch() {
        putInBeanWriter.bean = new FailOnNItemSink<String>(n: 5)
        // 5 is part of a chunk that failed before, so it shows up again
        // when the job restarts, the first thing the splitting reader
        // will do is a fetch from the delegate
        doFailingRun(1..5)

        putInBeanWriter.bean = new FailOnNItemSink<String>(n: Integer.MAX_VALUE)
        doSuccessfulRun(5..9)
    }

    @Test
    void resumesCorrectlyWithoutFetchingInitially() {

        putInBeanWriter.bean = new FailOnNItemSink<String>(n: 6)
        // when the job restarts, there will be no need to fetch something
        // from the delegate for the first item
        doFailingRun 1..6

        putInBeanWriter.bean = new FailOnNItemSink<String>(n: Integer.MAX_VALUE)
        doSuccessfulRun 7..9
    }

    @Test
    void resumesCorrectlyWithCache() {
        fieldSetSplittingItemReader.eagerLineListener = meanListener
        meanScaledItemSink.n  = 7
        putInBeanWriter.bean = meanScaledItemSink

        doFailingRun((1..4).collect { it / 2.5d } + (5..7).collect { it / 6.5d })

        meanScaledItemSink.items.clear() // remove items from last run
        meanScaledItemSink.n  = Integer.MAX_VALUE
        doSuccessfulRun((7..8).collect { it / 6.5d } + 9d / 9d)
    }
}
