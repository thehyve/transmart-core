package org.transmartproject.batch.i2b2.fact

import com.google.common.collect.ImmutableMap

/**
 * Values for the observation_fact.valueflag_cd.
 */
enum ValueFlag {

    HIGH('H'),      // numeric
    LOW('L'),       // numeric
    ABNORMAL('A'),  // text
    NORMAL('N'),    // numeric, text
    ENCRYPTED('E')  // blob, nlp

    final String textValue

    ValueFlag(String textValue) {
        this.textValue = textValue
    }

    private static final Map<String, ValueFlag> INDEX = {
        def builder = ImmutableMap.builder()
        values().each {
            builder.put(it.textValue, it)
        }
        builder.build()
    }()

    static ValueFlag forString(String s) {
        INDEX.get(s)
    }
}
