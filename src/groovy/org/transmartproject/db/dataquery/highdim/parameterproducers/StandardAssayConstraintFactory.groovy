package org.transmartproject.db.dataquery.highdim.parameterproducers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.assayconstraints.AssayIdListConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultOntologyTermConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultPatientSetConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultTrialNameConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.DisjunctionAssayConstraint
import org.transmartproject.db.dataquery.highdim.assayconstraints.NoopAssayConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.DisjunctionDataConstraint
import org.transmartproject.db.dataquery.highdim.dataconstraints.NoopDataConstraint

import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.convertToLong
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.getParam
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.processLongList
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.processStringList
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.validateParameterNames

/**
 * Created by glopes on 11/18/13.
 */
@Component
class StandardAssayConstraintFactory extends AbstractMethodBasedParameterFactory {

    @Autowired
    ConceptsResource conceptsResource

    @Autowired
    QueriesResource queriesResource

    private DisjunctionConstraintFactory disjunctionConstraintFactory =
            new DisjunctionConstraintFactory(DisjunctionAssayConstraint, NoopAssayConstraint)

    @ProducerFor(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT)
    AssayConstraint createOntologyTermConstraint(Map<String, Object> params) {
        if (params.size() != 1) {
            throw new InvalidArgumentsException("Expected exactly one parameter (concept_key), got $params")
        }

        def conceptKey = getParam params, 'concept_key', String

        OntologyTerm term
        try {
            term = conceptsResource.getByKey conceptKey
        } catch (NoSuchResourceException nse) {
            throw new InvalidArgumentsException(nse)
        }

        new DefaultOntologyTermConstraint(term: term)
    }

    @ProducerFor(AssayConstraint.PATIENT_SET_CONSTRAINT)
    AssayConstraint createPatientSetConstraint(Map<String, Object> params) {
        if (params.size() != 1) {
            throw new InvalidArgumentsException("Expected exactly one parameter (result_instance_id), got $params")
        }

        def resultInstanceId = getParam params, 'result_instance_id', Object
        resultInstanceId = convertToLong 'result_instance_id', resultInstanceId

        QueryResult result
        try {
            result = queriesResource.getQueryResultFromId resultInstanceId
        } catch (NoSuchResourceException nse) {
            throw new InvalidArgumentsException(nse)
        }

        new DefaultPatientSetConstraint(queryResult: result)
    }

    @ProducerFor(AssayConstraint.TRIAL_NAME_CONSTRAINT)
    AssayConstraint createTrialNameConstraint(Map<String, Object> params) {

        validateParameterNames(['name'], params)
        def name = getParam params, 'name', String

        new DefaultTrialNameConstraint(trialName: name)
    }

    @ProducerFor(AssayConstraint.ASSAY_ID_LIST_CONSTRAINT)
    AssayConstraint createAssayIdListConstraint(Map<String, Object> params) {
        validateParameterNames(['ids'], params)
        def ids = processLongList 'ids', params.ids

        new AssayIdListConstraint(ids: ids)
    }

    @ProducerFor(AssayConstraint.DISJUNCTION_CONSTRAINT)
    AssayConstraint createDisjunctionConstraint(Map<String, Object> params,
                                                Object createConstraint) {
        disjunctionConstraintFactory.
                createDisjunctionConstraint params, createConstraint
    }

}
