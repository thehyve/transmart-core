package org.transmartproject.core.ontology

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Metadata about the study.
 */
@Slf4j
@CompileStatic
class StudyMetadata {

    private static final JsonSlurper JSON_SLURPER = new JsonSlurper()

    Map<String, VariableMetadata> conceptCodeToVariableMetadata

    static StudyMetadata fromJson(String jsonText) {
        if (!jsonText?.trim()) {
            log.debug("Returns null metadata as the input is '${jsonText}'")
            return null
        }
        try {
            def json = JSON_SLURPER.parseText(jsonText)
            if (json != null && json instanceof Map) {
                return collectMetadataEntries((Map)json)
            }
        } catch (Exception e) {
            log.error("Failed parsing the folowing study metadata json: ${jsonText}", e)
        }
        return null
    }

    private static StudyMetadata collectMetadataEntries(Map json) {
        StudyMetadata studyMetadata = new StudyMetadata()
        if (json.conceptCodeToVariableMetadata && json.conceptCodeToVariableMetadata instanceof Map) {
            Map<String, VariableMetadata> conceptCodeToVariableMetadata = ((Map)json.conceptCodeToVariableMetadata).
                    collectEntries { Object code, Object varMeta ->
                        try {
                            return [(code.toString()): toVariableMetadata((Map)varMeta)]
                        } catch (Exception e) {
                            log.error("Can't parse the folowing variable metadata json for concept with code ${code}: ${varMeta}", e)
                        }
                        return [:]
                    }  as Map<String, VariableMetadata>
            studyMetadata.conceptCodeToVariableMetadata = conceptCodeToVariableMetadata
        }
        return studyMetadata
    }

    private static VariableMetadata toVariableMetadata(Map json) {
        def type = ((String)json.type)?.toUpperCase() as VariableDataType
        MissingValues missingValues
        if (type == VariableDataType.NUMERIC) {
            missingValues = toNumericMissingValues((Map)json.missingValues)
        } else {
            missingValues = toMissingValues((Map)json.missingValues)
        }
        new VariableMetadata(
                name: (String)json.name,
                type: type,
                measure: ((String)json.measure)?.toUpperCase() as Measure,
                description: (String)json.description,
                width: json.width as Integer,
                decimals: json.decimals as Integer,
                valueLabels: (((Map)json.valueLabels)?.collectEntries { Object key, Object value ->
                    [new BigDecimal(key.toString().trim()), value.toString()]
                } ?: [:]) as Map<BigDecimal, String>,
                missingValues: missingValues,
                columns: json.columns as Integer
        )
    }

    private static MissingValues toNumericMissingValues(Map json) {
        MissingValues result = toMissingValues(json)
        result?.with {
            values = values.collect { it as BigDecimal } as List<Object>
            upper = json.upper as BigDecimal
            lower =  json.lower as BigDecimal
        }
        return result
    }

    private static MissingValues toMissingValues(Map json) {
        if (json == null) {
            return null
        }
        List values = []
        if (json.value) {
            values.add(json.value)
        } else if (json.values) {
            json.values.each { values.add(it) }
        }
        return new MissingValues(
                values: values,
        )
    }

}
