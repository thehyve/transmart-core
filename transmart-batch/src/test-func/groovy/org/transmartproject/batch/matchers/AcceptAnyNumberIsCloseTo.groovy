package org.transmartproject.batch.matchers

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.number.IsCloseTo


/**
 * Because the Oracle driver seems to like to return double precision numbers a
 * BigDecimals, this matcher will work no matter the type of the item being
 * compared.
 */
class AcceptAnyNumberIsCloseTo extends BaseMatcher<Number> {

    private final BaseMatcher<Number> delegate

    Class desiredType

    private AcceptAnyNumberIsCloseTo(double value, double error) {
        desiredType = Double
        delegate = IsCloseTo.closeTo(value, error)
    }

    private AcceptAnyNumberIsCloseTo(BigDecimal value, BigDecimal error) {
        desiredType = BigDecimal
        delegate = IsCloseTo.closeTo(value, error)
    }

    static castingCloseTo(double value, double error) {
        new AcceptAnyNumberIsCloseTo(value, error)
    }

    static castingCloseTo(BigDecimal value, BigDecimal error) {
        new AcceptAnyNumberIsCloseTo(value, error)
    }

    @Override
    boolean matches(Object item) {
        delegate.matches(item.asType(desiredType))
    }

    @Override
    void describeTo(Description description) {
        delegate.describeTo(description)
    }
}
