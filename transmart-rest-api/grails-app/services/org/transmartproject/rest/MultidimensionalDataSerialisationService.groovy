package org.transmartproject.rest

import grails.transaction.Transactional
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.rest.protobuf.ObservationsSerializer

@Transactional
class MultidimensionalDataSerialisationService {

    /**
     * Serialises hypercube data to <code>out</code>.
     *
     * @param hypercube the hypercube to serialise.
     * @param format the output format. Supports JSON and PROTOBUF.
     * @param out the stream to serialise to.
     */
    void serialise(Hypercube hypercube, ObservationsSerializer.Format format, OutputStream out) {
        ObservationsSerializer builder = new ObservationsSerializer(hypercube, format, null)
        builder.write(out)
    }
}
