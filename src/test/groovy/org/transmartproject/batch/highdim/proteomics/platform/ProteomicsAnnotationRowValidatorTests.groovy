package org.transmartproject.batch.highdim.proteomics.platform

import org.junit.Before
import org.junit.Test
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.transmartproject.batch.highdim.platform.Platform

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.Assume.assumeThat

/**
 * {@link org.transmartproject.batch.highdim.proteomics.platform.ProteomicsAnnotationRowValidator}!
 */
class ProteomicsAnnotationRowValidatorTests {

    ProteomicsAnnotationRowValidator testee

    Platform platform = new Platform(
            id: 'PROT_ANNOT',
            title: 'Proteomics Test Annotation',
            organism: 'Homo Sapiens',
            markerType: 'PROTEOMICS')

    ProteomicsAnnotationRow sampleRow = new ProteomicsAnnotationRow(
            gplId: platform.id,
            probesetId: '4041',
            uniprotId: 'Q8WX92',
            chromosome: '9',
            startBp: 140149758,
            endBp: 140168000,
            organism: platform.organism)

    @Before
    void setUp() {
        platform
        testee = new ProteomicsAnnotationRowValidator(platformObject: platform)
    }

    private boolean sampleRowPasses() {
        !callValidate().hasErrors()
    }

    @Test
    void testBasicAnnotationPasses() {
        assert sampleRowPasses()
    }

    @Test
    void testGplIdMustMatch() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.gplId = 'prot_annot' // lowercase, should pass
        assertThat callValidate().hasErrors(), is(false)

        sampleRow.gplId = 'UNEXST'
        Errors errors = callValidate()

        assertThat errors, hasProperty('fieldErrorCount', equalTo(1))

        FieldError err = errors.getFieldError('gplId')
        assertThat err.code, is(equalTo('expectedConstant'))
    }

    @Test
    void testPlatFormMustMatch() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.organism = 'homo sapiens' // lowercase, should pass
        assertThat callValidate().hasErrors(), is(false)

        sampleRow.organism = 'hommmmmoo sapiens'// should fail this time
        FieldError err = callValidate().fieldError

        assertThat err, allOf(
                hasProperty('rejectedValue', equalTo(sampleRow.organism)),
                hasProperty('field', equalTo('organism')),
                hasProperty('code', equalTo('expectedConstant')))
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

    @Test
    void testChrPositionOptionality() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.chromosome = null
        sampleRow.startBp = null
        sampleRow.endBp = null

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testChrIsEmpty() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.chromosome = null

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('chromosome')),
                hasProperty('code', equalTo('required')))

        // empty string should also be rejected
        sampleRow.chromosome = ''
        err = callValidate().fieldError
        assertThat err, hasProperty('code', equalTo('required'))
    }

    @Test
    void testChrIsInvalid() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.chromosome = 'F'

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', equalTo(sampleRow.chromosome)),
                hasProperty('field', equalTo('chromosome')),
                hasProperty('code', equalTo('invalidChromosome')))

        sampleRow.chromosome = '-1'
        err = callValidate().fieldError
        assertThat err, hasProperty('code', equalTo('invalidChromosome'))
    }

    @Test
    void testStartBpIsEmpty() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.startBp = null

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('startBp')),
                hasProperty('code', equalTo('required')))
    }

    @Test
    void testStartBpIsNegative() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.startBp = -1

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', equalTo(sampleRow.startBp)),
                hasProperty('field', equalTo('startBp')),
                hasProperty('code', equalTo('connotBeNegative')))
    }

    @Test
    void testEndBpIsEmpty() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.endBp = null

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('endBp')),
                hasProperty('code', equalTo('required')))
    }

    @Test
    void testChrRangeIsInvalid() {
        assumeThat sampleRowPasses(), is(true)

        sampleRow.endBp = sampleRow.startBp - 1

        FieldError err = callValidate().fieldError
        assertThat err, allOf(
                hasProperty('rejectedValue', equalTo(sampleRow.endBp)),
                hasProperty('field', equalTo('endBp')),
                hasProperty('code', equalTo('invalidRange')))
    }

    private Errors callValidate() {
        Errors errors = new BeanPropertyBindingResult(sampleRow, "item")
        testee.validate(sampleRow, errors)
        errors
    }
}
