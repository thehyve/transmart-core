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

package org.transmartproject.db.dataquery.clinical.variables

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.clinical.ClinicalVariable
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.ontology.AbstractAcrossTrialsOntologyTerm
import org.transmartproject.db.ontology.I2b2

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.FOLDER
import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.LEAF

@Component /* not scanned; explicit bean definition */
class ClinicalVariableFactory {

    boolean disableAcrossTrials

    @Lazy def helper = {
        disableAcrossTrials ?
                null :
                new ClinicalVariableFactoryAcrossTrialsHelper()
    }()

    private Map<String, Closure<ClinicalVariable>> knownTypes =
            ImmutableMap.of(
                    ClinicalVariable.TERMINAL_CONCEPT_VARIABLE,
                    this.&createTerminalConceptVariable,
                    ClinicalVariable.CATEGORICAL_VARIABLE,
                    this.&createCategoricalVariable,
                    ClinicalVariable.NORMALIZED_LEAFS_VARIABLE,
                    this.&createNormalizedLeafsVariable
            )

    ClinicalVariable createClinicalVariable(Map<String, Object> params,
                                            String type) throws InvalidArgumentsException {
        def closure = knownTypes[type]

        if (!closure) {
            throw new InvalidArgumentsException("Invalid clinical variable " +
                    "type '$type', supported types are ${knownTypes.keySet()}")
        }

        if (params.size() != 1) {
            throw new InvalidArgumentsException("Expected exactly one parameter, " +
                    "got ${params.keySet()}")
        }

        String conceptCode,
               conceptPath
        if (params['concept_code']) {
            conceptCode = BindingUtils.getParam params, 'concept_code', String
        } else if (params['concept_path']) {
            conceptPath = BindingUtils.getParam params, 'concept_path', String
        } else {
            throw new InvalidArgumentsException("Expected the given parameter " +
                    "to be one of 'concept_code', 'concept_path', got " +
                    "'${params.keySet().iterator().next()}'")
        }

        closure.call((String) conceptCode, (String) conceptPath)
    }

    private TerminalClinicalVariable createTerminalConceptVariable(String conceptCode,
                                                                   String conceptPath) {
        if (helper?.isAcrossTrialsPath(conceptPath)) {
            return helper.createTerminalConceptVariable(conceptPath)
        }

        new TerminalConceptVariable(conceptCode: conceptCode,
                                    conceptPath: conceptPath)
    }

    private CategoricalVariable createCategoricalVariable(String conceptCode,
                                                          String conceptPath) {
        if (helper?.isAcrossTrialsPath(conceptPath)) {
            return helper.createCategoricalVariable(conceptPath)
        }

        List<ConceptDimension> descendantDimensions =
                descendantDimensions(conceptCode, conceptPath)

        // parent is the concept represented by the arguments
        def parent = descendantDimensions[0]
        if (descendantDimensions.size() == 1) {
            throw new InvalidArgumentsException('Concept with path ' +
                    "$conceptPath was supposed to be the container for a " +
                    "categorical variable, but instead no children were found")
        }
        def children = descendantDimensions[1..-1]
        def parentNumSlashes = parent.conceptPath.count('\\')

        def innerVariables = children.collect {
            def thisCount = it.conceptPath.count('\\')
            if (thisCount != parentNumSlashes + 1) {
                throw new InvalidArgumentsException("Concept with path " +
                        "'$conceptPath' does not seem to be a categorical " +
                        "variable because it has grandchildren (found " +
                        "concept path '${it.conceptPath}'")
            }

            createTerminalConceptVariable(it.conceptCode, null)
        }

        createCategoricalVariableFinal(parent.conceptPath, innerVariables)
    }

    private CategoricalVariable createCategoricalVariableFinal(String containerConceptPath,
                                                               List<TerminalConceptVariable> innerVariables) {
        new CategoricalVariable(conceptPath: containerConceptPath,
                                innerClinicalVariables: innerVariables)
    }

    private NormalizedLeafsVariable createNormalizedLeafsVariable(String conceptCode,
                                                                  String conceptPath) {
        if (helper?.isAcrossTrialsPath(conceptPath)) {
            return helper.createNormalizedLeafsVariable(conceptPath)
        }

        def resolvedConceptPath = resolveConceptPath(conceptCode, conceptPath)

        List<? extends OntologyTerm> terms = I2b2.withCriteria {
            'like' 'fullName', resolvedConceptPath.asLikeLiteral() + '%'
            order 'fullName', 'asc'
        }

        if (!terms) {
            throw new UnexpectedResultException("Could not find any path in " +
                    "i2b2 starting with $resolvedConceptPath")
        }
        if (terms[0].fullName != resolvedConceptPath) {
            throw new UnexpectedResultException("Expected first result to " +
                    "have concept path '$resolvedConceptPath', got " +
                    "'${terms[0].fullName}'")
        }

        Map<String, OntologyTerm> indexedTerms =
                terms.collectEntries {
                    [it.fullName, it]
                }

        def composingVariables = []

        Multimap<String, String> potentialCategorical = HashMultimap.create()
        // set of containers that cannot refer to categorical variables because
        // they have numerical children
        Set<String> blackListedCategorical = Sets.newHashSet()

        terms.findAll {
            LEAF in it.visualAttributes
        }.each {
            ConceptFullName conceptNameObj = new ConceptFullName(it.fullName)
            String parentName = conceptNameObj.parent?.toString()

            if (parentName && indexedTerms[parentName] &&
                    !(FOLDER in indexedTerms[parentName].visualAttributes)) {
                throw new IllegalStateException('Found parent concept that ' +
                        'is not a folder')
            }

            if (it.metadata?.okToUseValues) {
                // numeric leaf
                composingVariables <<
                        new TerminalConceptVariable(conceptPath: it.fullName)
                if (parentName) {
                    blackListedCategorical << parentName
                }
                return
            }

            // non-numeric leaf now

            if (conceptNameObj.length == 1) {
                // no parent, so not a candidate for categorical variable
                composingVariables <<
                        new TerminalConceptVariable(conceptPath: it.fullName)
                return
            }

            if (!indexedTerms.containsKey(parentName)) {
                // parent has NOT been selected
                composingVariables <<
                        new TerminalConceptVariable(conceptPath: it.fullName)
                return
            }

            potentialCategorical.put(parentName, it.fullName)
        }

        potentialCategorical.asMap().collect { String parentName,
                                               Collection<String> childrenNames ->
            List<TerminalConceptVariable> childrenVariables =
                    childrenNames.collect { String childrenName ->
                        new TerminalConceptVariable(conceptPath: childrenName)
                    }

            if (parentName in blackListedCategorical) {
                // blacklisted
                composingVariables.addAll childrenVariables
            } else {
                composingVariables <<
                        createCategoricalVariableFinal(parentName,
                                                  childrenVariables)
            }
        }

        composingVariables = composingVariables.sort { it.label }

        new NormalizedLeafsVariable(conceptPath: resolvedConceptPath,
                innerClinicalVariables: composingVariables)
    }

    private String resolveConceptPath(String conceptCode,
                                      String conceptPath) {
        def resolvedConceptPath = conceptPath
        if (!resolvedConceptPath) {
            assert conceptCode != null
            resolvedConceptPath =
                    ConceptDimension.findByConceptCode(conceptCode)?.conceptPath

            if (!resolvedConceptPath) {
                throw new InvalidArgumentsException(
                        "Could not find path of concept with code $conceptCode")
            }
        } else {
            if (!ConceptDimension.findByConceptPath(conceptPath)) {
                throw new InvalidArgumentsException("" +
                        "Could not find concept with path $conceptPath")
            }
        }

        resolvedConceptPath
    }

    List<ConceptDimension> descendantDimensions(String conceptCode,
                                                String conceptPath) {

        def resolvedConceptPath = resolveConceptPath(conceptCode, conceptPath)

        def result = ConceptDimension.withCriteria {
            like 'conceptPath', resolvedConceptPath.asLikeLiteral() + '%'

            order 'conceptPath', 'asc'
        }
        if (result[0]?.conceptPath != resolvedConceptPath) {
            throw new UnexpectedResultException('Expected first result to have ' +
                    "concept path '$resolvedConceptPath', got " +
                    "${result[0]?.conceptPath} instead")
        }

        result
    }

}

class ClinicalVariableFactoryAcrossTrialsHelper {

    boolean isAcrossTrialsPath(String conceptPath) {
        if (conceptPath == null) {
            return false
        }

        new ConceptFullName(conceptPath)[0] ==
                AbstractAcrossTrialsOntologyTerm.ACROSS_TRIALS_TOP_TERM_NAME
    }

    AcrossTrialsTerminalVariable createTerminalConceptVariable(String conceptPath) {
        new AcrossTrialsTerminalVariable(conceptPath: conceptPath)
    }

    ClinicalVariable createCategoricalVariable(String conceptPath) {
        throw new UnsupportedOperationException('Not supported yet')
    }

    ClinicalVariable createNormalizedLeafsVariable(String conceptPath) {
        throw new UnsupportedOperationException('Not supported yet')
    }
}
