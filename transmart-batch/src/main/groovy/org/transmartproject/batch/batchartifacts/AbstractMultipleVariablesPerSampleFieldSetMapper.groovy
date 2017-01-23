package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Lists
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Value
import org.springframework.validation.BindException
import org.transmartproject.batch.support.ScientificNotationFormat

import java.text.NumberFormat

/**
 * Maps the field set to the multiple fields of the multiple items.
 */
@Slf4j
abstract class AbstractMultipleVariablesPerSampleFieldSetMapper<T> implements MultipleItemsFieldSetMapper<T> {

    @Value('#{stepExecution}')
    StepExecution stepExecution

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

    NumberFormat numberFormat = new ScientificNotationFormat()

    @Override
    @SuppressWarnings(['CatchException', 'ExplicitLinkedHashMapInstantiation'])
    List<T> mapFieldSet(FieldSet fieldSet) throws BindException {
        Map<String, Map<String, String>> parsedHeader = stepExecution.executionContext
                .get(HeaderParsingLineCallbackHandler.PARSED_HEADER_OUT_KEY) as Map
        Map<String, T> instancesBySampleMap = new LinkedHashMap<String, T>() //predictable iteration order is essential
        String annotation = fieldSet.readString(0)

        def processColumn = { String columnName, Integer index ->
            def parsedColumnName = parsedHeader[columnName]

            if (!parsedColumnName) {
                throw new IllegalStateException("Column ${columnName} has not been parsed.")
            }

            String sampleCode = parsedColumnName.sample
            String variableName = parsedColumnName.suffix

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
            String value = fieldSet.readString(index)
            //skip blank values
            if (value?.trim()) {
                Closure setterClosure = fieldSetters[variableName]
                setterClosure(instance, value)
            }
        }

        fieldSet.names.eachWithIndex { String columnName, Integer index ->
            try {
                //skip the first column (annotation)
                if (index != 0) {
                    processColumn(columnName, index)
                }
            } catch (Exception e) {
                log.error "An error appears in '${columnName}' column.", e
                throw e
            }
        }

        Lists.newArrayList(instancesBySampleMap.values())
    }
}
