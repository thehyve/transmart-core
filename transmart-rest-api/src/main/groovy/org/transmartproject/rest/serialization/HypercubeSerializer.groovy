package org.transmartproject.rest.serialization

import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

/**
 * Commonalities between the JSON serialization in {@link JsonObservationsSerializer}
 * and the Protobuf serialization in {@link HypercubeProtobufSerializer}.
 */
abstract class HypercubeSerializer {

    abstract void write(Map args, Hypercube cube, OutputStream out)

    void write(Hypercube cube, OutputStream out) {
        write([:], cube, out)
    }
}
