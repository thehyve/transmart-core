package org.transmartproject.rest

import grails.transaction.Transactional
import org.transmartproject.db.dataquery2.HypercubeImpl
import org.transmartproject.rest.protobuf.ObservationsSerializer

@Transactional
class MultidimensionalDataSerialisationService {

    /**
     * Serialise hypercube data to <code>out</code>.
     *
     * @param hypercube the hypercube to serialise.
     * @param format the output format. Currently only 'json' is supported.
     * @param out the stream to serialise to.
     */
    def writeData(HypercubeImpl hypercube, String format = "json", OutputStream out) {
        if (!format.equalsIgnoreCase("json")) {
            throw new UnsupportedEncodingException("Serialization format ${format} is not supported")
        }
        ObservationsSerializer builder = new ObservationsSerializer(hypercube)
        builder.writeTo(out, format)
    }
}
