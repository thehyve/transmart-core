package org.transmartproject.core.binding

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import java.text.SimpleDateFormat

@CompileStatic
class BindingHelper {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    static ObjectMapper getObjectMapper() {
        def df = new SimpleDateFormat(DATE_TIME_FORMAT)
        df.setTimeZone(TimeZone.getTimeZone("UTC"))
        new ObjectMapper().setDateFormat(df)
    }

    static <T> void validate(T object) {
        if (object == null) {
            return
        }
        Set<ConstraintViolation<T>> errors = validator.validate(object)
        if (errors) {
            String sErrors = errors.collect { "${it.propertyPath.toString()}: ${it.message}" }.join('; ')
            throw new BindingException("${errors.size()} error(s): ${sErrors}", errors)
        }
    }

    static String toJson(Object o) {
        objectMapper.writeValueAsString(o)
    }

    /**
     * Create an object from a JSON input stream
     * using Jackson and validates the object.
     *
     * @param inputStream the JSON input stream
     * @return a validated object
     */
    static <T> T read(InputStream inputStream, Class<T> type) {
        if (inputStream == null) {
            return null
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper()
            def object = (T)objectMapper.readValue(inputStream, type)
            validate(object)
            object
        } catch (JsonProcessingException e) {
            throw new BindingException("Cannot parse parameters: ${e.message}", e)
        }
    }

    /**
     * Create a list of objects from a JSON string
     * using Jackson and validates the objects.
     *
     * @param src the JSON string
     * @return a list of validated objects
     */
    static <T> List<T> readList(String src, TypeReference<List<T>> typeReference) {
        if (src == null) {
            return null
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper()
            def elements = (List<T>)objectMapper.readValue(src, typeReference)
            validate(elements)
            elements
        } catch (JsonProcessingException e) {
            throw new BindingException("Cannot parse parameters: ${e.message}", e)
        }
    }

}
