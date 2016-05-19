package org.transmartproject.batch.highdim.cnv.data

import org.junit.Before
import org.junit.Test
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test... {@link org.transmartproject.batch.highdim.cnv.data.CnvDataValueValidator}!
 */
class CnvDataValueValidatorTests {

    CnvDataValueValidator testee
    CnvDataValue cnvDataValue
    Errors errors

    @Before
    void setUp() {
        testee = new CnvDataValueValidator()
        cnvDataValue = new CnvDataValue(
                regionName: 'region-name',
                sampleCode: 'sample-code',
                flag: 0,
                chip: 0.1d,
                segmented: 0.2d,

                probHomLoss: 0.1d,
                probLoss: 0.2d,
                probNorm: 0.4d,
                probGain: 0.2d,
                probAmp: 0.1d
        )
        errors = new BeanPropertyBindingResult(cnvDataValue, "item")
    }

    @Test
    void testValid() {
        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testRegionNameIsNull() {
        cnvDataValue.regionName = null

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('regionName')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testRegionNameIsEmptyString() {
        cnvDataValue.regionName = ''

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('regionName')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testRegionNameIsBlank() {
        cnvDataValue.regionName = ' '

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('regionName')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testSampleCodeIsNull() {
        cnvDataValue.sampleCode = null

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('sampleCode')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testSampleCodeIsEmptyString() {
        cnvDataValue.sampleCode = ''

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('sampleCode')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testSampleCodeIsBlank() {
        cnvDataValue.sampleCode = ' '

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('sampleCode')),
                hasProperty('code', equalTo('required'))
        ))
    }

    @Test
    void testFlagHasNotAllowedValue() {
        cnvDataValue.flag = -3

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(allOf(
                hasProperty('field', equalTo('flag')),
                hasProperty('code', equalTo('notAllowedValue'))
        ))
    }

    @Test
    void testProbabilitiesDoNotSumUpToOne() {
        cnvDataValue.probHomLoss = 1
        cnvDataValue.probGain = 0.5

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, contains(
                hasProperty('code', equalTo('sumOfProbabilitiesIsNotOne'))
        )
    }

    @Test
    void testIncorrectProbabilities() {
        cnvDataValue.probHomLoss = 3
        cnvDataValue.probLoss = 2
        cnvDataValue.probNorm = 1
        cnvDataValue.probGain = -2
        cnvDataValue.probAmp = -3

        List<ObjectError> errors = callValidate().allErrors

        assertThat errors, containsInAnyOrder(
                allOf(
                        hasProperty('field', equalTo('probHomLoss')),
                        hasProperty('code', equalTo('notAllowedValue'))),
                allOf(
                        hasProperty('field', equalTo('probLoss')),
                        hasProperty('code', equalTo('notAllowedValue'))),
                allOf(
                        hasProperty('field', equalTo('probGain')),
                        hasProperty('code', equalTo('notAllowedValue'))),
                allOf(
                        hasProperty('field', equalTo('probAmp')),
                        hasProperty('code', equalTo('notAllowedValue'))),
        )
    }

    @Test
    void testSomeProbabilitiesIsNull() {
        cnvDataValue.with {
            flag = 0
            probHomLoss = null
            probLoss = 0.25
            probNorm = 0.25
            probGain = 0.25
            probAmp = 0.25
        }

        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testAllProbabilitiesAreNull() {
        cnvDataValue.with {
            flag = 0
            probHomLoss = null
            probLoss = null
            probNorm = null
            probGain = null
            probAmp = null
        }

        assertThat callValidate().hasErrors(), is(false)
    }


    private Errors callValidate() {
        testee.validate(cnvDataValue, errors)
        errors
    }
}
