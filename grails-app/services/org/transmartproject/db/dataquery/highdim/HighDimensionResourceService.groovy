package org.transmartproject.db.dataquery.highdim

import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueryResult

class HighDimensionResourceService implements HighDimensionResource {

    Map<String, Closure<HighDimensionDataTypeResource>> dataTypeRegistry = new HashMap()

    @Override
    Set<String> getKnownTypes() {
        dataTypeRegistry.keySet()
    }

    @Override
    HighDimensionDataTypeResource getSubResourceForType(String dataTypeName)
            throws NoSuchResourceException {
        if (!dataTypeRegistry.containsKey(dataTypeName)) {
            throw new NoSuchResourceException("Unknown data type: $dataTypeName")
        }
        dataTypeRegistry[dataTypeName].call name: dataTypeName
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
        this.dataTypeRegistry[moduleName] = factory
        log.debug "Registered high dimensional data type module '$moduleName'"
    }


}
