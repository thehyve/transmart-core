package org.transmartproject.rest.marshallers

import grails.converters.JSON
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.springframework.stereotype.Component
import org.transmartproject.db.multidimquery.query.Constraint

@Slf4j
@CompileStatic
@Component
class ConstraintMarshaller implements ObjectMarshaller<JSON> {

    @Override
    boolean supports(Object object) {
        object != null && Constraint.isAssignableFrom(object.getClass())
    }

    private static final Set<String> combinationSubtypes = ['and', 'or', 'negation'] as Set<String>
    private static final Set<String> ignoredProperties = [
            'class', 'constraintName', 'errors', 'constraints', 'constraintsMap'] as Set<String>

    static final Map<String, Object> convertToMap(Constraint obj) {
        def result = [:] as Map<String, Object>
        String type = obj.properties.constraintName
        result.type = type
        obj.properties.each { key, value ->
            def propertyName = key as String
            if (value == null || (propertyName in ignoredProperties) || ((type in combinationSubtypes) && propertyName == 'operator')) {
                // skip
            } else {
                result[propertyName] = value
            }
        }
        result
    }

    @Override
    void marshalObject(Object object, JSON json) {
        Constraint constraint = (Constraint)object
        log.debug "Converting constraint of type ${constraint.properties.constraintName}..."
        Date t1 = new Date()
        Map<String, Object> mapRepresentation =
                convertToMap(constraint)
        Date t2 = new Date()
        log.debug "Convert to map took ${t2.time - t1.time} ms."
        json.value mapRepresentation
    }

}

