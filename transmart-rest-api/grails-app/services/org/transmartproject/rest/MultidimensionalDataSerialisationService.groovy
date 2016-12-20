package org.transmartproject.rest

import grails.transaction.Transactional
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.rest.serialization.AbstractObservationsSerializer
import org.transmartproject.rest.serialization.JsonObservationsSerializer
import org.transmartproject.rest.serialization.ProtobufObservationsSerializer

@Transactional
class MultidimensionalDataSerialisationService {

    /**
     * Type to represent the requested serialization format.
     */
    static enum Format {
        JSON('application/json'),
        PROTOBUF('application/x-protobuf'),
        NONE('none')

        private String format

        Format(String format) {
            this.format = format
        }

        public static Format from(String format) {
            Format f = values().find { it.format == format }
            if (f == null) throw new Exception("Unknown format: ${format}")
            f
        }

        public String toString() {
            format
        }
    }

    /**
     * Serialises hypercube data to <code>out</code>.
     *
     * @param hypercube the hypercube to serialise.
     * @param format the output format. Supports JSON and PROTOBUF.
     * @param out the stream to serialise to.
     */
    void serialise(Hypercube hypercube, Format format, OutputStream out) {
        AbstractObservationsSerializer serializer
        switch (format) {
            case Format.JSON:
                serializer = new JsonObservationsSerializer(hypercube)
                break
            case Format.PROTOBUF:
                serializer = new ProtobufObservationsSerializer(hypercube, null)
                break
            default:
                throw new Exception("Unsupported format: ${format}")
        }
        serializer.write(out)
    }
}
