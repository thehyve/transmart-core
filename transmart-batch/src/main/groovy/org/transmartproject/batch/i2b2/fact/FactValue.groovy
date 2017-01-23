package org.transmartproject.batch.i2b2.fact
/**
 * Represents the data (excluding associations, dates and metadata) on an
 * observation_fact row.
 */
interface FactValue {

    /**
     * Value for observation_fact.tval_num
     *
     * Max 255 characters
     */
    String getTextValue()

    /**
     * Value for observation_fact.nval_num
     *
     * 18 digits before the comma, 5 after
     */
    BigDecimal getNumberValue()

    /**
     * Value for observation_fact.valtype_cd
     */
    FactDataType getDataType()

    /**
     * Value for observation_fact.valueflag_cd
     */
    ValueFlag getValueFlag()

    /**
     * Value for observation_fact.observation_blob
     */
    String getBlob()
}
