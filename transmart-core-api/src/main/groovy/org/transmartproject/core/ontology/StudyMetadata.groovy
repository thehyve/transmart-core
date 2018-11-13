package org.transmartproject.core.ontology

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * Metadata about the study.
 */
@Slf4j
class StudyMetadata {

    private static final JsonSlurper JSON_SLURPER = new JsonSlurper()

    Map<String, VariableMetadata> conceptCodeToVariableMetadata
    TabularRepresentationMetadata defaultTabularRepresentation

    static StudyMetadata fromJson(String jsonText) {
        if (!jsonText?.trim()) {
            log.debug("Returns null metadata as the input is '${jsonText}'")
            return null
        }
        try {
            def json = JSON_SLURPER.parseText(jsonText)
            if (json != null) {
                return collectMetadataEntries(json)
            }
        } catch (Exception e) {
            log.error("Failed parsing the folowing study metadata json: ${jsonText}", e)
        }
        return null
    }

    private static StudyMetadata collectMetadataEntries(json) {
        StudyMetadata studyMetadata = new StudyMetadata()
        if(json.conceptCodeToVariableMetadata){
            Map<String, VariableMetadata> conceptCodeToVariableMetadata = json.conceptCodeToVariableMetadata.
                    collectEntries { String code, Object varMeta ->
                        try {
                            return [(code): toVariableMetadata(varMeta)]
                        } catch (Exception e) {
                            log.error("Can't parse the folowing variable metadata json for concept with code ${code}: ${varMeta}", e)
                        }
                        return [:]
                    }
            studyMetadata.conceptCodeToVariableMetadata = conceptCodeToVariableMetadata
        }
        if(json.defaultTabularRepresentation){
            try {
                def defaultTabularRepresentation = toTabularRepresentationMetadata(json.defaultTabularRepresentation)
                studyMetadata.defaultTabularRepresentation = defaultTabularRepresentation
            } catch (Exception e) {
                log.error("Can't parse the folowing tabular representation metadata json: ${json.defaultTabularRepresentation}", e)
                studyMetadata.defaultTabularRepresentation = []
            }
        }
        return studyMetadata
    }

    private static VariableMetadata toVariableMetadata(Object json) {
        def type = json.type?.toUpperCase() as VariableDataType
        def missingValues
        if (type == VariableDataType.NUMERIC) {
            missingValues = toNumericMissingValues(json.missingValues)
        } else {
            missingValues = toMissingValues(json.missingValues)
        }
        new VariableMetadata(
                name: json.name,
                type: type,
                measure: json.measure?.toUpperCase() as Measure,
                description: json.description,
                width: json.width as Integer,
                decimals: json.decimals as Integer,
                valueLabels: json.valueLabels?.collectEntries { String key, String value ->
                    [new BigDecimal(key.trim()), value]
                } ?: [:],
                missingValues: missingValues,
                columns: json.columns as Integer
        )
    }

    private static MissingValues toNumericMissingValues(json) {
        MissingValues result = toMissingValues(json)
        result?.with {
            values = values.collect { it as BigDecimal }
            upper = json.upper as BigDecimal
            lower =  json.lower as BigDecimal
        }
        return result
    }

    private static MissingValues toMissingValues(json) {
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

    private static TabularRepresentationMetadata toTabularRepresentationMetadata(Object json) {
        new TabularRepresentationMetadata(
                rowDimensions: json.rowDimensions,
                columnDimensions: json.columnDimensions
        )
    }

}
