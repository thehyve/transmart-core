/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimBuilder

class HighDimDataService {

    HighDimensionResource highDimensionResourceService
    Closure<TabularResult> resultTransformer //optional closure to transform/inspect the high dim data results

    /**
     * Retrieves the highdim data for the given conceptKey/dataType/projectionName
     * and writes the protobuf result in the given output stream.
     * If no projection is specified, we will use the default one for the given dataType.
     *
     * @param conceptKey key of the concept to retrieve highdim for
     * @param dataType highdim data type
     * @param projectionName name of projection (or null for the default)
     * @param out output stream to write protobuf to
     */
    void write(String conceptKey,
               String dataType,
               String projectionName,
               Map assayConstraintsSpec,
               Map dataConstraintsSpec,
               OutputStream out) {

        HighDimensionDataTypeResource typeResource =
                highDimensionResourceService.getSubResourceForType(dataType)

        String proj = projectionName ?: getDefaultProjectionFor(dataType, typeResource.supportedProjections)

        Projection projection = typeResource.createProjection(proj)

        List<AssayConstraint> assayConstraints = [
                typeResource.createAssayConstraint(
                        AssayConstraint.ONTOLOGY_TERM_CONSTRAINT,
                        concept_key: conceptKey)] +
                assayConstraintsSpec.collect { String type, List instances ->
                    instances.collect { Map params ->
                        typeResource.createAssayConstraint(params, type)
                    }
                }.flatten()

        List<DataConstraint> dataConstraints = dataConstraintsSpec.collect {
                String type, List instances ->
            instances.collect { Map params ->
                typeResource.createDataConstraint(params, type)
            }
        }.flatten()

        TabularResult tabularResult = typeResource.retrieveData(
                assayConstraints, dataConstraints, projection)

        if (resultTransformer) {
            tabularResult = resultTransformer(tabularResult)
        }

        try {
            HighDimBuilder.write(projection, tabularResult, out)
        } finally {
            tabularResult.close() //closing the tabular result, no matter what
        }
    }

    Map<HighDimensionDataTypeResource, Collection<Assay>>  getAvailableHighDimResources(String conceptKey) {

        AssayConstraint assayConstraint = highDimensionResourceService.createAssayConstraint(
                AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: conceptKey)

        highDimensionResourceService.getSubResourcesAssayMultiMap([assayConstraint])
    }

    /**
     * Returns DEFAULT_REAL_PROJECTION if supported, ot the first projection in the given set if not.
     *
     * @param dataType
     * @param supportedProjections
     * @return
     */
    private static String getDefaultProjectionFor(String dataType, Set<String> supportedProjections) {
        if (Projection.DEFAULT_REAL_PROJECTION in supportedProjections) {
            return Projection.DEFAULT_REAL_PROJECTION
        } else {
            return supportedProjections.first()
        }
    }

    Projection getProjection(String dataType, String name) {
        highDimensionResourceService.getSubResourceForType(dataType).createProjection(name)
    }

}
