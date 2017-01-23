package org.transmartproject.batch.support

import com.google.common.base.Charsets

import java.nio.charset.Charset

/**
 * Trait for validator whose value of the <code>lengthOf</code> method can be
 * configured.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
trait ConfigurableLengthSemanticsTrait {

    boolean useBytes = false

    Charset charset = Charsets.UTF_8 /* only relevant if useBytes */

    Integer lengthOf(String s) {
        if (!useBytes) {
            s.length()
        } else {
            s.getBytes(charset).length
        }
    }
}
