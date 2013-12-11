package org.transmartproject.db.dataquery.highdim

import com.google.common.collect.HashMultimap
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.db.dataquery.highdim.assayconstraints.DefaultPatientSetConstraint

class HighDimensionResourceService implements HighDimensionResource {

    private static final int MAX_CACHED_DATA_TYPE_RESOURCES = 50
    private static final int MAX_CACHED_PLATFORM_MAPPINGS = 200

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
    Map<HighDimensionDataTypeResource, Collection<Assay>> getSubResourcesAssayMultiMap(QueryResult queryResult) {
        DefaultPatientSetConstraint constraint =
            new DefaultPatientSetConstraint(queryResult: queryResult)

        List<DeSubjectSampleMapping> assays = DeSubjectSampleMapping.withCriteria {
            platform {
                // fetch platforms
            }
            constraint.addConstraintsToCriteria delegate

            isNotNull 'platform'
        } /* one row per assay */

        HashMultimap multiMap = HashMultimap.create()
        for (Assay a in assays) {
            String dataTypeName =
                    cachingDataTypeResourceForPlatform.call a.platform
            if (!dataTypeName) {
                continue
            }

            multiMap.put cachingDataTypeResourceProducer.call(dataTypeName), a
        }

        multiMap.asMap()
    }

    @Lazy Closure<String> cachingDataTypeResourceForPlatform = { Platform p ->
        dataTypeRegistry.keySet().
                find { String dataTypeName ->
                    cachingDataTypeResourceProducer.call(dataTypeName).
                            matchesPlatform(p)
                } /* may return null */
    }.memoizeAtMost(MAX_CACHED_PLATFORM_MAPPINGS)

    @Lazy Closure<HighDimensionDataTypeResourceImpl> cachingDataTypeResourceProducer =
        this.&getSubResourceForType.memoizeAtMost(MAX_CACHED_DATA_TYPE_RESOURCES)

    /**
     * Register a new high dimensional type. Factory is a closure that takes a
     * map with one entry: name: <module name>
     * @param moduleName
     * @param factory
     */
    void registerHighDimensionDataTypeModule(String moduleName,
                                             Closure<HighDimensionDataTypeResource> factory) {
        this.dataTypeRegistry[moduleName] = factory
        log.debug "Registered high dimensional data type module '$moduleName'"
    }


}
