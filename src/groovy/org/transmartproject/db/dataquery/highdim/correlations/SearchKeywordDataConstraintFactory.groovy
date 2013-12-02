package org.transmartproject.db.dataquery.highdim.correlations

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.MapBasedParameterFactory
import org.transmartproject.db.search.SearchKeywordCoreDb

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

     private List processList(String paramName, Object obj, Closure closure) {
        if (!(obj instanceof List)) {
            throw new InvalidArgumentsException("Parameter '$paramName' " +
                    "is not a List, got a ${obj.getClass()}")
        }

        if (obj.isEmpty()) {
            throw new InvalidArgumentsException('Value of parameter ' +
                    "'$paramName' is an empty list; this is unacceptable")
        }

        obj.collect { closure.call it }
    }

    private List<String> processStringList(String paramName, Object obj) {
        processList paramName, obj, {
            if (it instanceof String) {
                it
            } else if (it instanceof Number) {
                it.toString()
            } else {
                throw new InvalidArgumentsException("Parameter '$paramName' " +
                        "is not a list of String; found in a list an object with " +
                        "type ${it.getClass()}")
            }
        }
    }

    private List<Long> processLongList(String paramName, Object obj) {
        processList paramName, obj, {
            if (it instanceof String) {
                if (!it.isLong()) {
                    throw new InvalidArgumentsException("Parameter '$paramName' " +
                            "is not a list of longs; found in a list an object " +
                            "with type ${it.getClass()}")
                } else {
                    it as Long
                }
            } else if (it instanceof Number) {
                ((Number) it).longValue()
            } else {
                throw new InvalidArgumentsException("Parameter '$paramName' " +
                        "is not a list of longs; found in a list an object " +
                        "with type ${it.getClass()}")
            }
        }
    }

}
