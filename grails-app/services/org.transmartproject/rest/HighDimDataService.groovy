package org.transmartproject.rest

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimBuilder

class HighDimDataService {

    def highDimensionResourceService

    /**
     * Retrieves the highdim data for the given conceptKey/dataType/projectionName
     * and writes the protobuf result in the given output stream.
     *
     * @param conceptKey key of the concept to retrieve highdim for
     * @param dataType highdim data type
     * @param projectionName name of projection
     * @param out output stream to write protobuf to
     */
    void write(String conceptKey, String dataType, String projectionName, OutputStream out) {

        HighDimensionDataTypeResource typeResource =
                highDimensionResourceService.getSubResourceForType(dataType)

        Projection projection = typeResource.createProjection(projectionName)

        AssayConstraint assayConstraint = 
                typeResource.createAssayConstraint(AssayConstraint.ONTOLOGY_TERM_CONSTRAINT, concept_key: conceptKey)

        TabularResult tabularResult =  typeResource.retrieveData([assayConstraint], [], projection)

        try {
            HighDimBuilder.write(projection, tabularResult, out)
        } finally {
            tabularResult.close() //closing the tabular result, no matter what
        }
    }

}
