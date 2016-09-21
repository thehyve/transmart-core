package org.transmartproject.batch.batchartifacts

import org.junit.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.ParseException
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource

import java.nio.charset.StandardCharsets

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test the {@link MultipleItemsLineItemReader} class.
 */
class MultipleItemsLineItemReaderTests {

    private static final double DELTA = 0.005d

    MultipleItemsLineItemReader<TestBean> testee

    @Test
    void testSuccess() {
        initTesteeWithContent(
                'annotation\tsampl1.dfld\tsampl1.ifld\tsampl2.dfld\tsampl2.ifld\n' +
                        'annot1\t1.2\t10\t3.4\t15\n' +
                        'annot2\t5.6\t20\t7.8\t25\n'
        )

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

    @Test(expected = IllegalArgumentException)
    void testNoFieldSetMapperSpecified() {
        testee = new MultipleItemsLineItemReader(resource: [] as Resource, multipleItemsFieldSetMapper: null)
        testee.afterPropertiesSet()
    }

    @Test(expected = IllegalArgumentException)
    void testNoResourceSpecified() {
        testee = new MultipleItemsLineItemReader(resource: null,
                multipleItemsFieldSetMapper: [] as MultipleItemsFieldSetMapper)
        testee.afterPropertiesSet()
    }

    @Test(expected = ItemStreamException)
    void testDuplicatesInTheHeader() {
        initTesteeWithContent(
                'annotation\tsampl1.dfld\tsampl1.ifld\tsampl1.dfld\tsampl1.ifld\n' +
                        'annot1\t1.2\t10\t1.2\t10\n'
        )

        testee.read()
    }

    @Test(expected = ParseException)
    void testUnParsableHeader() {
        initTesteeWithContent(
                'annotation\tsampl1.dfld\tunparsableheadername\n' +
                        'annot1\t1.2\ttest\n'
        )

        testee.read()
    }

    void initTesteeWithContent(String content) {
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        Resource resource = new InputStreamResource(is)
        testee = new MultipleItemsLineItemReader(
                resource: resource,
                multipleItemsFieldSetMapper: new AbstractMultipleVariablesPerSampleFieldSetMapper() {
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
        )
        testee.afterPropertiesSet()
        testee.open(new ExecutionContext())
    }
}
