package org.transmartproject.batch.highdim.datastd

import org.junit.Before
import org.junit.Test
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.transmartproject.batch.highdim.platform.Platform

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test... {@link PlatformValidator}
 */
class PlatformValidatorTests {

    PlatformValidator testee
    PlatformTestBean platformTestBean
    Errors errors

    class PlatformTestBean implements PlatformOrganismSupport {
        String gplId
        String organism
    }

    @Before
    void setUp() {
        def platform = new Platform(id: 'GPL570', organism: 'Homo Sapiens')
        testee = new PlatformValidator(platformObject: platform)
        platformTestBean = new PlatformTestBean(
                gplId: platform.id,
                organism: platform.organism
        )
        errors = new BeanPropertyBindingResult(platformTestBean, "item")
    }

    @Test
    void testValid() {
        assertThat callValidate().hasErrors(), is(false)
    }

    @Test
    void testGplIdMustMatch() {
        platformTestBean.gplId = 'gpl570' // lowercase, should pass
        assertThat callValidate().hasErrors(), is(false)

        platformTestBean.gplId = 'GOK580_foobar'
        Errors errors = callValidate()

        assertThat errors, hasProperty('fieldErrorCount', equalTo(1))

        FieldError err = errors.getFieldError('gplId')
        assertThat err.code, is(equalTo('expectedConstant'))
    }

    @Test
    void testPlatFormMustMatch() {
        platformTestBean.organism = 'homo sapiens' // lowercase, should pass
        assertThat callValidate().hasErrors(), is(false)

        platformTestBean.organism = 'hommmmmoo sapiens'// should fail this time
        FieldError err = callValidate().fieldError

        assertThat err, allOf(
                hasProperty('rejectedValue', equalTo(platformTestBean.organism)),
                hasProperty('field', equalTo('organism')),
                hasProperty('code', equalTo('expectedConstant')))
    }

    private Errors callValidate() {
        testee.validate(platformTestBean, errors)
        errors
    }
}
