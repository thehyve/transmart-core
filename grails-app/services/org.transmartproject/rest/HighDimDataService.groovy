package org.transmartproject.rest

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.db.dataquery.highdim.acgh.AcghModule
import org.transmartproject.rest.protobuf.HighDimBuilder

class HighDimDataService {

    HighDimensionResourceService highDimensionResourceService

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
    void write(String conceptKey, String dataType, String projectionName, OutputStream out) {

        HighDimensionDataTypeResource typeResource =
                highDimensionResourceService.getSubResourceForType(dataType)

        String proj = projectionName ?: getDefaultProjectionFor(dataType)

        Projection projection = typeResource.createProjection(proj)

        AssayConstraint assayConstraint = 
                typeResource.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: conceptKey)

        TabularResult tabularResult =  typeResource.retrieveData([assayConstraint], [], projection)

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

    static String getDefaultProjectionFor(String dataType) {
        switch (dataType) {
            case 'acgh':
                return AcghModule.ACGH_VALUES_PROJECTION
            default:
                return Projection.DEFAULT_REAL_PROJECTION
        }
    }

}
