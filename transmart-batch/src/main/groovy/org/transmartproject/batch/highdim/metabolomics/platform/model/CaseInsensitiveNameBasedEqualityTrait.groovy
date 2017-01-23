package org.transmartproject.batch.highdim.metabolomics.platform.model

import com.google.common.base.Ascii
import com.google.common.collect.ComparisonChain

/**
 * Equals, hashCode and compareTo() implementation based on the ascii
 * lower case version of the name property.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
trait CaseInsensitiveNameBasedEqualityTrait implements Comparable {

    abstract String getName()

    boolean equals(Object other) {
        if (this.getClass() != other.getClass()) {
            return false
        }

        CaseInsensitiveNameBasedEqualityTrait o =
                (CaseInsensitiveNameBasedEqualityTrait) other

        Objects.equals(lowercase(this.name), lowercase(o.name))
    }

    int hashCode() {
        Objects.hash(lowercase(this.name))
    }

    int compareTo(Object other) {
        if (other.getClass() != this.getClass()) {
            throw new IllegalArgumentException(
                    "Can only be compared against ${this.getClass()}")
        }

        CaseInsensitiveNameBasedEqualityTrait o =
                (CaseInsensitiveNameBasedEqualityTrait) other

        ComparisonChain.start()
                .compare(lowercase(this.name), lowercase(o.name))
                .result()
    }

    private static String lowercase(String s) {
        s != null ? Ascii.toLowerCase(s) : null
    }
}
