package org.transmartproject.db.dataquery.highdim

import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueryResult

class HighDimensionResourceService implements HighDimensionResource {

    Map<String, Closure<HighDimensionDataTypeResource>> knownDataTypes = new HashMap()

    @Override
    HighDimensionDataTypeResource getSubResourceForType(String dataTypeName)
            throws NoSuchResourceException {
        if (!knownDataTypes.containsKey(dataTypeName)) {
            throw new NoSuchResourceException("Unknown data type: $dataTypeName")
        }
        knownDataTypes[dataTypeName].call name: dataTypeName
    }

    @Override
    Map<HighDimensionDataTypeResource, Long> getSubResourcesForPatientSet(QueryResult queryResult) {
        throw new RuntimeException('Not yet implemented')
    }

    /**
     * Register a new high dimensional type. Factory is a closure that takes a
     * map with one entry: name: <module name>
     * @param moduleName
     * @param factory
     */
    void registerHighDimensionDataTypeModule(String moduleName,
                                             Closure<HighDimensionDataTypeModule> factory) {
        this.knownDataTypes[moduleName] = factory
        log.debug "Registered high dimensional data type module '$moduleName'"
    }


}
