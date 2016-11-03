package org.transmartproject.rest.protobuf

import com.google.common.collect.ImmutableList
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube

/**
 * Created by piotrzakrzewski on 02/11/2016.
 */
public class ObservationsBuilder {

    def serialize(Hypercube hypercube) {
        ImmutableList<Dimension> dimensions = hypercube.getDimensions();
        def dimMsg = dimensions.collect() { dim ->
            def builder = Observations.dimensionDeclaration.newBuilder()
            if (!dim.density.equals(Dimension.Density.SPARSE)) {
                builder.setIsDense(true)
            }
            builder.setName(dim.toString())
            builder.setType(Observations.dimensionDeclaration.Type.STRING)
            builder.build()
        }
        dimMsg
    }
}
