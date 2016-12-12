package org.transmartproject.rest.marshallers

import org.transmartproject.export.Datatypes

class DataTypeSerializationHelper  extends AbstractHalOrJsonSerializationHelper<Datatypes>{

    final Class targetType = Datatypes
    final String collectionName = 'datatypes'

    @Override
    Map<String, Object> convertToMap(Datatypes datatypes) {
        def cohortInfoList = []
        def cohortsMap = [:]
        datatypes.ontologyTermsMap.each { id, terms ->
            terms.collect { term ->
                if (id in cohortsMap.keySet()) {
                    cohortsMap[id].add([subjects: term.patients.collect({ it.id }), conceptPath: term.fullName])
                } else {
                    cohortsMap[id] = [[subjects: term.patients.collect({ it.id }), conceptPath: term.fullName]]
                }
            }
        }
        cohortsMap.each{ key, value ->
            cohortInfoList.add([concepts:value])
        }
        def datatypeMap = [dataType:datatypes.dataType,
                           dataTypeCode: datatypes.dataTypeCode,
                           cohorts:cohortInfoList]
    }

}
