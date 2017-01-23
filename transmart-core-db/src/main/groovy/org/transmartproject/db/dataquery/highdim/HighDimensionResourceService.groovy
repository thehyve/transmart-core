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

package org.transmartproject.db.dataquery.highdim

import com.google.common.collect.HashMultimap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.db.dataquery.highdim.assayconstraints.AssayCriteriaConstraint
import org.transmartproject.db.dataquery.highdim.parameterproducers.StandardAssayConstraintFactory

@Component
class HighDimensionResourceService implements HighDimensionResource {

    private static final int MAX_CACHED_DATA_TYPE_RESOURCES = 50
    private static final int MAX_CACHED_PLATFORM_MAPPINGS = 200

    /*
     * I couldn't get this field autowired with this class in
     * grails-app/services (or manually wired though doWithSpring for that
     * matter).
     *
     * When doing dependency injection for ConceptsResourceService, spring was
     * using the injection metadata for HighDimensionResourceService.
     * findAutowiringMetadata(String beanName, Class<?> clazz) is called with
     * '(inner bean)', ConceptsResourceService as parameters, and then does a
     * lookup on a cache whose key is preferably the bean name.
     * Only if the bean name is empty does it use the class name, excpet the
     * the bean name is '(inner bean)', which I'm guessing is used with other
     * inner beans.
     */
    @Autowired
    StandardAssayConstraintFactory assayConstraintFactory

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
    Map<HighDimensionDataTypeResource, Collection<Assay>> getSubResourcesAssayMultiMap(
            List<AssayConstraint> assayConstraints) {

        List<DeSubjectSampleMapping> assays = DeSubjectSampleMapping.withCriteria {
            platform {
                // fetch platforms
            }

            assayConstraints.each { AssayCriteriaConstraint constraint ->
                constraint.addToCriteria owner.delegate
            }

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

    @Override
    AssayConstraint createAssayConstraint(Map<String, Object> params, String name) {
        def res = assayConstraintFactory.createFromParameters(name, params,
                this.&createAssayConstraint)

        if (!res) {
            throw new InvalidArgumentsException(
                    "Unsupported assay constraint: $name")
        }
        res
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
     *
     * The factory should produce objects that, if not identical, are at least
     * equal to each other.
     * @param moduleName
     * @param factory
     */
    void registerHighDimensionDataTypeModule(String moduleName,
                                             Closure<HighDimensionDataTypeResource> factory) {
        this.dataTypeRegistry[moduleName] = factory
        HighDimensionResourceService.log.debug "Registered high dimensional data type module '$moduleName'"
    }


}
