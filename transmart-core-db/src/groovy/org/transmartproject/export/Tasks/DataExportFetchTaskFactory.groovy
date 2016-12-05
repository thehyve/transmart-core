package org.transmartproject.export.Tasks

import com.google.common.collect.Lists
import grails.util.Environment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.ontology.ConceptsResource

@Component
class DataExportFetchTaskFactory implements  ApplicationContextAware {

    public static final String CONCEPT_KEYS_PARAMETER_NAME = 'conceptKeys'
    public static final String ASSAY_CONSTRAINTS_PARAMETER_NAME = 'assayConstraints'
    public static final String DATA_CONSTRAINTS_PARAMETER_NAME = 'dataConstraints'
    public static final String PROJECTION_PARAMETER_NAME = 'projection'
    public static final String RESULT_INSTANCE_IDS_PARAMETER_NAME = 'resultInstanceIds'

    ApplicationContext applicationContext

    @Autowired
    private ConceptsResource conceptsResource


    // we don't support several constraints of the same type yet
    private boolean constraintsOK(Object value) {
        if (value == null) {
            return true
        }
        (value instanceof Map) &&
                value.keySet().every { it instanceof  String } &&
                value.values().every { it instanceof Map }
    }

    @Override
    DataExportFetchTask createTask(Map<String, Object> arguments) {
        def conceptKeysArg = arguments[CONCEPT_KEYS_PARAMETER_NAME]
        def ridsArg =
                arguments[RESULT_INSTANCE_IDS_PARAMETER_NAME]

        if (!conceptKeysArg) {
            throw new InvalidArgumentsException(
                    "Parameter $CONCEPT_KEYS_PARAMETER_NAME not passed")
        }
        if (!(conceptKeysArg instanceof Map)) {
            throw new InvalidArgumentsException(
                    "Expected $CONCEPT_KEYS_PARAMETER_NAME to be a map")
        }
        // normalize keys and values to strings
        conceptKeysArg = conceptKeysArg.collectEntries() { k, v ->
            try {
                [k as String, conceptsResource.getByKey(v as String)]
            } catch (Exception ex) {
                // should be IllegalArgumentException | NoSuchResourceException
                // but that's broken in the version of Groovy used
                throw new InvalidArgumentsException("The string '$v' " +
                        "(with label prefix '$k') is not a valid concept " +
                        "key: $ex.message", ex)
            }
        } /* is Map<String, OntologyTerm> after this */

        // one null is allowed (either s1 or s2 empty)
        if (ridsArg == null) {
            throw new InvalidArgumentsException(
                    "Parameter $RESULT_INSTANCE_IDS_PARAMETER_NAME not passed")
        }
        if (!(ridsArg instanceof List)) {
            throw new InvalidArgumentsException("Parameter " +
                    "$RESULT_INSTANCE_IDS_PARAMETER_NAME must be a list")
        }
        if (ridsArg.any { it && !(it as String).isLong() }) {
            throw new InvalidArgumentsException(
                    "Parameter $RESULT_INSTANCE_IDS_PARAMETER_NAME can only " +
                            "have integer or null values, got: " +
                            ridsArg.find { it && !(it as String).isLong() })
        }
        if (Lists.newArrayList(ridsArg).unique().size() < ridsArg.size()) {
            "Parameter $RESULT_INSTANCE_IDS_PARAMETER_NAME has " +
                    "duplicate values"
        }
        if (ridsArg.findAll().size() == 0) {
            if (Environment.current != Environment.TEST) {
                throw new InvalidArgumentsException("Parameter $ridsArg " +
                        "cannot be an empty list or have only nulls")
            } else { // is test environment
                ridsArg = [null]
            }
        }

        applicationContext.getBean(DataExportFetchTask).with {
            ontologyTerms = conceptKeysArg

            resultInstanceIds = ridsArg.collect { it as Long }

            assayConstraints = arguments[ASSAY_CONSTRAINTS_PARAMETER_NAME]
            dataConstraints = arguments[DATA_CONSTRAINTS_PARAMETER_NAME]
            projection = arguments[PROJECTION_PARAMETER_NAME]

            if (!constraintsOK(assayConstraints)) {
                throw new InvalidArgumentsException(
                        'assay constraints need to be a map string -> map')
            }
            if (!constraintsOK(dataConstraints)) {
                throw new InvalidArgumentsException(
                        'data constraints need to be a map string -> map')
            }

            it
        }
    }
}
