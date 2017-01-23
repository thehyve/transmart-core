package org.transmartproject.batch.i2b2.fact

import com.google.common.collect.ImmutableMap

/**
 * Data types for i2b2 facts (valtype_cd column in observation_fact).
 */
enum FactDataType {

    TEXT('T'),   /* max 255 */
    BLOB('B'),
    NUMBER('N'), /* including the signs <, <=, = (default), >, >= */
    NLP('NLP')

    final String valueTypeCode

    FactDataType(String valueTypeCode) {
        this.valueTypeCode = valueTypeCode
    }

    private final static Map<String, FactDataType> INDEX = ImmutableMap.of(
            'text', TEXT,
            'blob', BLOB,
            'number', NUMBER,
            'nlp', NLP,)

    /**
     * Map from the strings used in the column mapping files.
     * @param s the string representation
     * @return the data type or null
     */
    final static FactDataType fromInputFileRepresentation(String s) {
        INDEX[s.toLowerCase(Locale.ENGLISH)]
    }

}
