package org.transmartproject.batch.batchartifacts

import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.validation.BindException

import java.util.regex.Matcher

/**
 * Maps the field set to the multiple fields of the multiple items.
 */
abstract class AbstractMultipleVariablesPerSampleFieldSetMapper<T> implements MultipleItemsFieldSetMapper<T> {

    private static final COLUMN_NAME_TUPLE_PATTERN = /^(.*)\.([^.]+)$/

    abstract T newInstance(String annotation, String sampleCode)

    abstract Map<String, Closure> getFieldSetters()

    @Override
    Collection<T> mapFieldSet(FieldSet fieldSet) throws BindException {
        Map<String, T> instancesBySampleMap = [:]
        String annotation = fieldSet.values[0]
        fieldSet.names[1..-1].eachWithIndex { String columnName, Integer index ->
            Matcher matcher = columnName =~ COLUMN_NAME_TUPLE_PATTERN
            String sampleCode = matcher[0][1]
            String variable = matcher[0][2]

            if (!fieldSetters.containsKey(variable)) {
                throw new UnsupportedOperationException("Variable ${variable} is not supported.")
            }

            T instance
            if (instancesBySampleMap.containsKey(sampleCode)) {
                instance = instancesBySampleMap[sampleCode]
            } else {
                instance = newInstance(annotation, sampleCode)
                instancesBySampleMap[sampleCode] = instance
            }
            fieldSetters[variable](instance, fieldSet.values[index + 1])
        }

        instancesBySampleMap.values()
    }
}
