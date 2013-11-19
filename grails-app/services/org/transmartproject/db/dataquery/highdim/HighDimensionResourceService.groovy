package org.transmartproject.db.dataquery.highdim

import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueryResult

class HighDimensionResourceService implements HighDimensionResource {

    Map<String, HighDimensionDataTypeModule> knownDataTypes = new HashMap()

    @Override
    HighDimensionDataTypeResource getSubResourceForType(String s) throws NoSuchResourceException {
        if (!knownDataTypes.containsKey(s)) {
            throw new NoSuchResourceException("Unknown data type: $s")
        }
        new HighDimensionDataTypeResourceImpl(knownDataTypes[s])
    }

    @Override
    Map<HighDimensionDataTypeResource, Long> getSubResourcesForPatientSet(QueryResult queryResult) {
        throw new RuntimeException('Not yet implemented')
    }

    void registerHighDimensionDataTypeModule(HighDimensionDataTypeModule module) {
        this.knownDataTypes[module.name] = module
        log.debug "Registered high dimensional data type module '$module.name': $module"
    }


}
