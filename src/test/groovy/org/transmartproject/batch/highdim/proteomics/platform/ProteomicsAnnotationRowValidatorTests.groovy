package org.transmartproject.batch.highdim.proteomics.platform

import org.junit.Before
import org.junit.Test
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assume.assumeThat

/**
 * {@link org.transmartproject.batch.highdim.proteomics.platform.ProteomicsAnnotationRowValidator}!
 */
class ProteomicsAnnotationRowValidatorTests {

    ProteomicsAnnotationRowValidator testee

    ProteomicsAnnotationRow sampleRow = new ProteomicsAnnotationRow(
            gplId: 'PROT_ANNOT',
            probesetId: '4041',
            uniprotId: 'Q8WX92',
            chromosome: '9',
            startBp: 140149758,
            endBp: 140168000,
            organism: 'Homo Sapiens')

    @Before
    void setUp() {
        testee = new ProteomicsAnnotationRowValidator()
    }

    private boolean sampleRowPasses() {
        !callValidate().hasErrors()
    }

    @Test
    void testBasicAnnotationPasses() {
        assert sampleRowPasses()
    }

    @Test
    void testProbesetIdIsMandatory() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.probesetId = null

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('probesetId')),
                hasProperty('code', equalTo('required')))

        // empty string should also be rejected
        sampleRow.probesetId = ''
        err = callValidate().fieldError
        assertThat err, hasProperty('code', equalTo('required'))
    }

    @Test
    void testUniprotIdIsMandatory() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.uniprotId = null

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('uniprotId')),
                hasProperty('code', equalTo('required')))

        // empty string should also be rejected
        sampleRow.uniprotId = ''
        err = callValidate().fieldError
        assertThat err, hasProperty('code', equalTo('required'))
    }

    private Errors callValidate() {
        Errors errors = new BeanPropertyBindingResult(sampleRow, "item")
        testee.validate(sampleRow, errors)
        errors
    }
}
