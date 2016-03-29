package org.transmartproject.batch.highdim.mrna.platform

import org.junit.Before
import org.junit.Test
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.transmartproject.batch.highdim.platform.Platform

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assume.assumeThat

/**
 * Test... {@link MrnaAnnotationRowValidator}!
 */
class MrnaAnnotationRowValidatorTests {

    MrnaAnnotationRowValidator testee

    Platform platform = new Platform(
            id: 'GPL570',
            title: 'Affymetrix Human Genome U133A 2.0 Array',
            organism: 'Homo Sapiens',
            markerType: 'Gene Expression')

    MrnaAnnotationRow sampleRow = new MrnaAnnotationRow(
            gplId: platform.id,
            probeName: '1552280_at',
            genes: 'TIMD4',
            entrezIds: '91937',
            organism: platform.organism)

    @Before
    void setUp() {
        testee = new MrnaAnnotationRowValidator()
    }

    private boolean sampleRowPasses() {
        !callValidate().hasErrors()
    }

    @Test
    void testBasicAnnotationPasses() {
        assert sampleRowPasses()
    }

    @Test
    void testProbeNameIsMandatory() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.probeName = null

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('probeName')),
                hasProperty('code', equalTo('required')))

        // empty string should also be rejected
        sampleRow.probeName = ''
        err = callValidate().fieldError
        assertThat err, hasProperty('code', equalTo('required'))
    }

    @Test
    void testEmptyGeneOneEntrezId() {
        assumeThat sampleRowPasses(), is(true)

        // null or '' is assumed as one value (null)
        sampleRow.genes = ''
        assertThat callValidate().hasErrors(), is(false)

        sampleRow.genes = null
        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testEmptyEntrezIdOneGene() {
        assumeThat sampleRowPasses(), is(true)

        // null or '' is assumed as one value (null)
        sampleRow.entrezIds = ''
        assertThat callValidate().hasErrors(), is(false)

        sampleRow.entrezIds = null
        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testEmptyGeneAndEntrezId() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.genes = null
        sampleRow.entrezIds = null

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testMultipleGenes() {
        sampleRow.genes = 'GENE1 /// GENE2'
        sampleRow.entrezIds = '1234 /// 2423'

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testMultipleGenesMissingId() {
        sampleRow.genes = 'GENE1 /// GENE2'
        sampleRow.entrezIds = '1234 /// '

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testNonNumericEntrezId() {
        sampleRow.entrezIds = 'dsfgd'

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', equalTo(sampleRow.entrezIds)),
                hasProperty('field', equalTo('entrezIds')),
                hasProperty('code', equalTo('expectedSeparatedLongs')))
    }

    @Test
    void testMistmatchedNumberOfGenesAndIds() {
        sampleRow.genes = 'GENE1 /// GENE2'
        sampleRow.entrezIds = '1234'

        ObjectError err = callValidate().globalError
        assertThat err, hasProperty('code', equalTo('sizeMismatch'))
    }

    private Errors callValidate() {
        Errors errors = new BeanPropertyBindingResult(sampleRow, "item")
        testee.validate(sampleRow, errors)
        errors
    }
}
