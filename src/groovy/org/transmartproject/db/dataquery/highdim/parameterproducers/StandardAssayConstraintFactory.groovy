/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import org.transmartproject.db.dataquery.highdim.assayconstraints.*

import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.*

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
            new DisjunctionConstraintFactory(DisjunctionAssayCriteriaConstraint, NoopAssayCriteriaConstraint)

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

        new DefaultOntologyTermCriteriaConstraint(term: term)
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

        new DefaultPatientSetCriteriaConstraint(queryResult: result)
    }

    @ProducerFor(AssayConstraint.TRIAL_NAME_CONSTRAINT)
    AssayConstraint createTrialNameConstraint(Map<String, Object> params) {

        validateParameterNames(['name'], params)
        def name = getParam params, 'name', String

        new DefaultTrialNameCriteriaConstraint(trialName: name)
    }

    @ProducerFor(AssayConstraint.ASSAY_ID_LIST_CONSTRAINT)
    AssayConstraint createAssayIdListConstraint(Map<String, Object> params) {
        validateParameterNames(['ids'], params)
        def ids = processLongList 'ids', params.ids

        new AssayIdListCriteriaConstraint(ids: ids)
    }

    @ProducerFor(AssayConstraint.DISJUNCTION_CONSTRAINT)
    AssayConstraint createDisjunctionConstraint(Map<String, Object> params,
                                                Object createConstraint) {
        disjunctionConstraintFactory.
                createDisjunctionConstraint params, createConstraint
    }

}
