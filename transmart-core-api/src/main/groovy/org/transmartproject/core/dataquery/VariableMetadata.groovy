package org.transmartproject.core.dataquery

import groovy.json.JsonSlurper

/**
 * Metadata about the variable/column taken from SPSS.
 */
class VariableMetadata {

    private static final JsonSlurper JSON_SLURPER = new JsonSlurper()

    /**
     * Data type of the concept
     */
    VariableDataType type
    /**
     * Levels of measurement
     */
    Measure measure
    /**
     * Description of the variable
     */
    String description
    /**
     * Refers to how many characters a value can hold
     */
    Integer width
    /**
     * Representation. Number of digits in fractional part.
     */
    Integer decimals
    /**
     * How many columns to represent on the screen.
     */
    Integer columns
    /**
     * Values index. e.g. 1=Female, 2=Male -1=Not provided
     */
    Map<BigDecimal, String> valueLabels = [:]
    /**
     * Contains indexes that represent missing values. Usually negative values. e.g. -1
     */
    MissingValues missingValues


    static VariableMetadata fromJson(String jsonText) {
        if (!jsonText?.trim()) {
            return null
        }
        def json = JSON_SLURPER.parseText(jsonText)
        new VariableMetadata(
                type: json.type?.toUpperCase() as VariableDataType,
                measure: json.measure?.toUpperCase() as Measure,
                description: json.description,
                width: json.width as Integer,
                decimals: json.decimals as Integer,
                valueLabels: json.valueLabels?.collectEntries { String key, String value ->
                    [new BigDecimal(key), value] } ?: [:],
                missingValues: toMissingValues(json.missingValues),
                columns: json.columns as Integer
        )
    }

    private static toMissingValues(json) {
        if (json == null) {
            return null
        }
        List<BigDecimal> values = []
        if (json.value) {
            values.add(json.value as BigDecimal)
        } else if (json.values) {
            json.values.each { values.add(it as BigDecimal) }
        }
        new MissingValues(
                upper: json.upper as BigDecimal,
                lower: json.lower as BigDecimal,
                values: values,
        )
    }
}
