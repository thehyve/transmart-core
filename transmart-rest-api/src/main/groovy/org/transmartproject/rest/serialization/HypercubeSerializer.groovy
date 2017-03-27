/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.serialization

import org.transmartproject.core.multidimquery.Hypercube

/**
 * Commonalities between the JSON serialization in {@link HypercubeJsonSerializer}
 * and the Protobuf serialization in {@link HypercubeProtobufSerializer}.
 */
abstract class HypercubeSerializer {

    abstract void write(Map args, Hypercube cube, OutputStream out)

    void write(Hypercube cube, OutputStream out) {
        write([:], cube, out)
    }
}
