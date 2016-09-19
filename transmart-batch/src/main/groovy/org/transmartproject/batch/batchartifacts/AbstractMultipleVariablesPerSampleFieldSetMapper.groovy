package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Lists
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.validation.BindException

import java.util.regex.Matcher

/**
 * Maps the field set to the multiple fields of the multiple items.
 */
@Slf4j
abstract class AbstractMultipleVariablesPerSampleFieldSetMapper<T> implements MultipleItemsFieldSetMapper<T> {

    private static final COLUMN_NAME_TUPLE_PATTERN = /^(.*)\.([^.]+)$/

    /**
     * Creates an instance of the type T and initializes it with annotation and sample code.
     */
    abstract T newInstance(String annotation, String sampleCode)

    /**
     * The key of the map is a variable name and the value is a closure.
     * The closure receives an object of type T and a string value and meant optionally convert the value
     * and set it to the object.
     * @return Map of setters.
     */
    abstract Map<String, Closure> getFieldSetters()

    @Override
    @SuppressWarnings(['CatchException', 'ExplicitLinkedHashMapInstantiation'])
    List<T> mapFieldSet(FieldSet fieldSet) throws BindException {
        Map<String, T> instancesBySampleMap = new LinkedHashMap<String, T>() //predictable iteration order is essential
        String annotation = fieldSet.readString(0)

        def processColumn = { String columnName, Integer index ->
            //skip the first column (annotation)
            if (index == 0) {
                return
            }

            Matcher matcher = (columnName =~ COLUMN_NAME_TUPLE_PATTERN)
            if (!matcher) {
                throw new ParseException("Could not parse the header name: ${columnName}." +
                        " The header name has to be of the following format <sample code>.<variable name>.")
            }

            String sampleCode = matcher[0][1]
            String variableName = matcher[0][2]

            if (!fieldSetters.containsKey(variableName)) {
                throw new UnsupportedOperationException("Variable ${variableName} is not supported.")
            }

            T instance
            if (instancesBySampleMap.containsKey(sampleCode)) {
                instance = instancesBySampleMap[sampleCode]
            } else {
                instance = newInstance(annotation, sampleCode)
                instancesBySampleMap[sampleCode] = instance
            }
            String value = fieldSet.readRawString(index)
            Closure setterClosure = fieldSetters[variableName]
            setterClosure(instance, value)
        }

        fieldSet.names.eachWithIndex { String columnName, Integer index ->
            try {
                processColumn(columnName, index)
            } catch (Exception e) {
                log.error "An error appears in '${columnName}' column.", e
                throw e
            }
        }

        Lists.newArrayList(instancesBySampleMap.values())
    }
}
