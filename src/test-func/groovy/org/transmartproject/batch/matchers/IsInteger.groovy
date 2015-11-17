package org.transmartproject.batch.matchers

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * Oracle returns BigDecimals for columns with NUMBER type and 0 digits to the
 * right of the decimal point. Use this matcher to hide the differences
 * (the postrgesql schema + driver causing a Long to be returned).
 */
class IsInteger extends TypeSafeMatcher<Number> {

    private BigInteger requiredValue

    static IsInteger isIntegerNumber(Long l) {
        new IsInteger(requiredValue: l as BigInteger)
    }

    static IsInteger isIntegerNumber(Integer l) {
        new IsInteger(requiredValue: l as BigInteger)
    }

    static IsInteger isIntegerNumber(BigInteger l) {
        new IsInteger(requiredValue: l)
    }

    @Override
    protected boolean matchesSafely(Number item) {
        describeMismatchSafelyGen(item, new Description.NullDescription())
    }

    @Override
    void describeTo(Description description) {
        description.appendText("a Number with integer value ")
                .appendValue(requiredValue)
    }

    @Override
    protected void describeMismatchSafely(Number item, Description mismatchDescription) {
        describeMismatchSafelyGen(item, mismatchDescription)
    }

    @SuppressWarnings('UnnecessaryModOne')
    private boolean describeMismatchSafelyGen(Number item, Description mismatchDescription) {
        BigInteger itemTransformed

        if (item instanceof Character ||
                item instanceof Long ||
                item instanceof Short ||
                item instanceof Integer) {
            itemTransformed = (item as Long) as BigInteger
        } else if (item instanceof Double) {
            if (item % 1 != 0d) {
                mismatchDescription.appendValue(item)
                        .appendText('is not whole')
                return false
            }
            itemTransformed = item as BigInteger
        } else if (item instanceof BigDecimal) {
            if (item.remainder(BigDecimal.ONE)) {
                mismatchDescription.appendValue(item)
                        .appendText('is not whole')
                return false
            }
            itemTransformed = item as BigInteger
        } else if (item instanceof BigInteger) {
            itemTransformed = item
        } else {
            mismatchDescription.appendValue(item)
                    .appendText('is not a supported type of number (')
                    .appendText(item.getClass().name)
                    .appendText(')')
            return false
        }

        if (itemTransformed != requiredValue) {
            super.describeMismatchSafely(item, mismatchDescription)
            return false
        }

        true
    }
}
