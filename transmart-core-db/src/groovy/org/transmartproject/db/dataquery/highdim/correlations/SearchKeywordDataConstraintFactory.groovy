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

package org.transmartproject.db.dataquery.highdim.correlations

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.processLongList
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.processStringList

class SearchKeywordDataConstraintFactory implements DataRetrievalParameterFactory {

    CorrelationTypesRegistry correlationTypesRegistry

    String targetType

    String alias,
           property

    @Delegate
    DataRetrievalParameterFactory innerFactory


    SearchKeywordDataConstraintFactory(CorrelationTypesRegistry correlationTypesRegistry,
                                       String targetType,
                                       String alias,
                                       String property) {
        this.correlationTypesRegistry = correlationTypesRegistry
        this.targetType = targetType
        this.alias = alias
        this.property = property

        def searchKeywordIdsConstraint = searchKeywordIdsConstraint()
        def dataTypeSpecificConstraints = dataTypeSpecificConstraints()

        def builder = ImmutableMap.builder()

        builder.put(searchKeywordIdsConstraint[0],
                    searchKeywordIdsConstraint[1])

        dataTypeSpecificConstraints.each {
            builder.put(it[0], it[1])
        }

        innerFactory = new MapBasedParameterFactory(builder.build())
    }

    /* search_keyword_ids constraint */
    List searchKeywordIdsConstraint() {
        Set<CorrelationType> allCorrelations = correlationTypesRegistry.
                getCorrelationTypesForTargetType(targetType)
        [
                DataConstraint.SEARCH_KEYWORD_IDS_CONSTRAINT,
                this.&createSearchKeywordIdsConstraint.curry(allCorrelations)
        ]
    }

    DataConstraint createSearchKeywordIdsConstraint(Set<CorrelationType> allCorrelations,
                                                    Map<String, Object> params) {
        if (params.size() != 1) {
            throw new InvalidArgumentsException(
                    "Expected exactly 1 parameter here; got $params.size()")
        }
        if (params.keyword_ids == null) {
            throw new InvalidArgumentsException(
                    "Could not find the parameter 'keyword_ids'")
        }
        List<Long> keywordIds = processLongList 'keyword_ids', params.keyword_ids

        SearchKeywordDataConstraint.createForSearchKeywordIds(
                entityAlias: alias,
                propertyToRestrict: property,
                correlationTypes: allCorrelations,
                keywordIds)
    }

    /* source data type specific constraints */
    List dataTypeSpecificConstraints() {
        correlationTypesRegistry.getConstraintsForTargetType(targetType).collect {
            String constraintName,
            CorrelationType correlationType ->
            [
                    constraintName,
                    this.&createSourceTypeSpecificConstraint.
                            curry(correlationType)
            ]
        }
    }

    DataConstraint createSourceTypeSpecificConstraint(CorrelationType correlation,
                                                      Map<String, Object> params) {
        List<SearchKeywordCoreDb> keywords

        if (params.size() != 1) {
            throw new InvalidArgumentsException(
                    "Expected exactly 1 parameter here; got $params.size()")
        }

        if (params.containsKey('names')) {
            List names = processStringList 'names', params.names

            keywords = SearchKeywordCoreDb.findAllByKeywordInListAndDataCategory(
                            names, correlation.sourceType)

            if (keywords.isEmpty()) {
                throw new InvalidArgumentsException("No search keywords " +
                        "of the category $correlation.sourceType match with " +
                        "name in list $names")
            }
        } else if (params.containsKey('ids')) {
            // these ids are the 'external' ids, not the search keyword PKs

            List ids = processStringList 'ids', params.ids

            def uniqueIds = ids.collect { "$correlation.sourceType:$it" as String }
            keywords = SearchKeywordCoreDb.findAllByUniqueIdInListAndDataCategory(
                            uniqueIds, correlation.sourceType)

            if (keywords.isEmpty()) {
                throw new InvalidArgumentsException("No search keywords " +
                        "of the category $correlation.sourceType match " +
                        "UNIQUE_ID in list $uniqueIds")
            }
        } else {
            def paramName = Iterables.getFirst params.keySet(), null

            throw new InvalidArgumentsException("Invalid parameter: $paramName")
        }

        SearchKeywordDataConstraint.createForSearchKeywords(
                keywords,
                entityAlias:        alias,
                propertyToRestrict: property,
                correlationTypes:   ImmutableSet.of(correlation))
    }

}
