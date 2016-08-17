package org.transmartproject.batch.batchartifacts

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.batch.test.MetaDataInstanceFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.transmartproject.batch.beans.StepBuildingConfigurationTrait
import org.transmartproject.batch.highdim.datastd.RowItemsProcessor
import org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataMultipleVariablesPerSampleFieldSetMapper

import java.nio.charset.StandardCharsets

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test the {@link MultipleItemsLineItemReader} class.
 */
class MultipleItemsLineItemReaderTests {

    private static final double DELTA = 0.005d

    MultipleItemsLineItemReader<TestBean> testee

    @Rule
    @SuppressWarnings('PublicInstanceField')
    public final ExpectedException exception = ExpectedException.none()

    @Test
    void testSuccess() {
        initTestee()

        TestBean item1 = testee.read()
        TestBean item2 = testee.read()
        TestBean item3 = testee.read()
        TestBean item4 = testee.read()
        TestBean item5 = testee.read()

        assertThat([item1, item2, item3, item4, item5], contains(
                allOf(
                        hasProperty('annotation', equalTo('annot1')),
                        hasProperty('sampleCode', equalTo('sampl1')),
                        hasProperty('doubleField', closeTo(1.2d, DELTA)),
                        hasProperty('intField', equalTo(10)),
                ),
                allOf(
                        hasProperty('annotation', equalTo('annot1')),
                        hasProperty('sampleCode', equalTo('sampl2')),
                        hasProperty('doubleField', closeTo(3.4d, DELTA)),
                        hasProperty('intField', equalTo(15)),
                ),
                allOf(
                        hasProperty('annotation', equalTo('annot2')),
                        hasProperty('sampleCode', equalTo('sampl1')),
                        hasProperty('doubleField', closeTo(5.6d, DELTA)),
                        hasProperty('intField', equalTo(20)),
                ),
                allOf(
                        hasProperty('annotation', equalTo('annot2')),
                        hasProperty('sampleCode', equalTo('sampl2')),
                        hasProperty('doubleField', closeTo(7.8d, DELTA)),
                        hasProperty('intField', equalTo(25)),
                ),
                nullValue()
        ))
    }

    @Test
    void testNoFieldSetMapperSpecified() {
        exception.expect(IllegalArgumentException)
        exception.expectMessage(
                equalTo('mapper has to be specified'))

        testee = new MultipleItemsLineItemReader(itemStreamReader: [:] as AbstractItemCountingItemStreamItemReader,
                multipleItemsFieldSetMapper: null)
        testee.afterPropertiesSet()
    }

    @Test
    void testNoStreamReaderSpecified() {
        exception.expect(IllegalArgumentException)
        exception.expectMessage(
                equalTo('item reader has to be specified'))

        testee = new MultipleItemsLineItemReader(itemStreamReader: null,
                multipleItemsFieldSetMapper: new RnaSeqDataMultipleVariablesPerSampleFieldSetMapper())
        testee.afterPropertiesSet()
    }

    @Test
    void testPreProcess() {
        initTestee()
        testee.rowItemsProcessor = new RowItemsProcessor<TestBean>() {
            @Override
            List<TestBean> process(List<TestBean> rowItems) {
                rowItems.collect {
                    it.intField = it.intField * 2
                    it
                }
            }
        }

        TestBean item1 = testee.read()
        TestBean item2 = testee.read()
        TestBean item3 = testee.read()
        TestBean item4 = testee.read()
        TestBean item5 = testee.read()

        assertThat([item1, item2, item3, item4, item5], contains(
                allOf(
                        hasProperty('annotation', equalTo('annot1')),
                        hasProperty('sampleCode', equalTo('sampl1')),
                        hasProperty('doubleField', closeTo(1.2d, DELTA)),
                        hasProperty('intField', equalTo(20)),
                ),
                allOf(
                        hasProperty('annotation', equalTo('annot1')),
                        hasProperty('sampleCode', equalTo('sampl2')),
                        hasProperty('doubleField', closeTo(3.4d, DELTA)),
                        hasProperty('intField', equalTo(30)),
                ),
                allOf(
                        hasProperty('annotation', equalTo('annot2')),
                        hasProperty('sampleCode', equalTo('sampl1')),
                        hasProperty('doubleField', closeTo(5.6d, DELTA)),
                        hasProperty('intField', equalTo(40)),
                ),
                allOf(
                        hasProperty('annotation', equalTo('annot2')),
                        hasProperty('sampleCode', equalTo('sampl2')),
                        hasProperty('doubleField', closeTo(7.8d, DELTA)),
                        hasProperty('intField', equalTo(50)),
                ),
                nullValue()
        ))
    }

    void initTestee() {
        String content = 'annotation\tsampl1.dfld\tsampl1.ifld\tsampl2.dfld\tsampl2.ifld\n' +
                'annot1\t1.2\t10\t3.4\t15\n' +
                'annot2\t5.6\t20\t7.8\t25\n'
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        Resource resource = new InputStreamResource(is)
        def fieldSetMapper = new AbstractMultipleVariablesPerSampleFieldSetMapper() {
            @Override
            TestBean newInstance(String annotation, String sampleCode) {
                new TestBean(annotation: annotation, sampleCode: sampleCode)
            }

            @Override
            Map<String, Closure> getFieldSetters() {
                [
                        dfld: { TestBean bean, String value -> bean.doubleField = value as Double },
                        ifld: { TestBean bean, String value -> bean.intField = value as Integer },
                ]
            }
        }
        fieldSetMapper.stepExecution = MetaDataInstanceFactory.createStepExecution()
        fieldSetMapper.stepExecution.executionContext.put(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY,
                [
                        'sampl1.dfld': [sample: 'sampl1', suffix: 'dfld'],
                        'sampl1.ifld': [sample: 'sampl1', suffix: 'ifld'],
                        'sampl2.dfld': [sample: 'sampl2', suffix: 'dfld'],
                        'sampl2.ifld': [sample: 'sampl2', suffix: 'ifld'],
                ])

        testee = new MultipleItemsLineItemReader(
                multipleItemsFieldSetMapper: fieldSetMapper,
                itemStreamReader: ([] as StepBuildingConfigurationTrait).tsvFileReader(
                        resource,
                        linesToSkip: 1,
                        columnNames: 'auto',
                )
        )
        testee.afterPropertiesSet()
        testee.open(new ExecutionContext())
    }
}
