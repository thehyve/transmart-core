package org.transmartproject.batch.highdim.datastd

import org.junit.Before
import org.junit.Test
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError
import org.transmartproject.batch.highdim.platform.Platform

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test... {@link ChromosomalRegionValidator}
 */
class ChromosomalRegionValidatorTests {

    ChromosomalRegionValidator testee
    ChromosomalRegionSupport chromosomalRegionBean
    Errors errors

    class ChromosomalRegionTestBean implements ChromosomalRegionSupport {
        String chromosome
        Long startBp
        Long endBp
    }

    @Before
    void setUp() {
        def platform = new Platform(genomeRelease: 'hg18')
        testee = new ChromosomalRegionValidator(optionalDefinition: true, platformObject: platform)
        chromosomalRegionBean = new ChromosomalRegionTestBean(
                chromosome: 'X',
                startBp: 1000,
                endBp: 2000
        )
        errors = new BeanPropertyBindingResult(chromosomalRegionBean, "item")
    }

    @Test
    void testValid() {
        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testZerosAllowedInTheRange() {
        chromosomalRegionBean.startBp = 0
        chromosomalRegionBean.endBp = 0

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testOptionalDefinition() {
        chromosomalRegionBean.chromosome = null
        chromosomalRegionBean.startBp = null
        chromosomalRegionBean.endBp = null

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testCompulsoryDefinition() {
        testee = new ChromosomalRegionValidator(optionalDefinition: false)

        chromosomalRegionBean.chromosome = null
        chromosomalRegionBean.startBp = null
        chromosomalRegionBean.endBp = null

        assertThat callValidate().hasErrors(), is(true)
    }

    @Test
    void testChromosomeIsNull() {
        chromosomalRegionBean.chromosome = null

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('chromosome')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testChromosomeIsBlank() {
        chromosomalRegionBean.chromosome = ' '

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('chromosome')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testChromosomeIsIllegal() {
        chromosomalRegionBean.chromosome = 'Z'

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('rejectedValue', equalTo(chromosomalRegionBean.chromosome)),
                hasProperty('field', equalTo('chromosome')),
                hasProperty('code', equalTo('invalidChromosome'))
        ))
    }

    @Test
    void testChromosomeIsIllegal2Symbols() {
        chromosomalRegionBean.chromosome = 'XY'

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('chromosome')),
                hasProperty('code', equalTo('invalidChromosome'))
        ))
    }

    @Test
    void testStartBpIsNull() {
        chromosomalRegionBean.startBp = null

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('startBp')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testStartBpIsNegative() {
        chromosomalRegionBean.startBp = -1

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('rejectedValue', equalTo(chromosomalRegionBean.startBp)),
                hasProperty('field', equalTo('startBp')),
                hasProperty('code', equalTo('connotBeNegative'))
        ))
    }

    @Test
    void testEndBpIsNull() {
        chromosomalRegionBean.endBp = null

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('rejectedValue', nullValue()),
                hasProperty('field', equalTo('endBp')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testEndBpIsNegative() {
        chromosomalRegionBean.endBp = -1

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('rejectedValue', equalTo(chromosomalRegionBean.endBp)),
                hasProperty('field', equalTo('endBp')),
                hasProperty('code', equalTo('invalidRange'))
        ))
    }

    @Test
    void testGenReleaseMandatoryIfChrInfoSpecified() {
        testee.platformObject.genomeRelease = null

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('code', equalTo('genReleaseMandatoryIfChrInfoSpecified'))
        ))
    }

    private Errors callValidate() {
        testee.validate(chromosomalRegionBean, errors)
        errors
    }
}
