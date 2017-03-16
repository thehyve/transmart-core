/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer
import org.transmartproject.rest.serialization.HypercubeSerializer
import org.transmartproject.rest.serialization.HypercubeJsonSerializer

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
     */@CompileStatic
    void serialise(Hypercube hypercube, Format format, OutputStream out, boolean pack) {
        HypercubeSerializer serializer
        switch (format) {
            case Format.JSON:
                serializer = new HypercubeJsonSerializer()
                break
            case Format.PROTOBUF:
                serializer = new HypercubeProtobufSerializer()
                break
            default:
                throw new Exception("Unsupported format: ${format}")
        }
        serializer.write(hypercube, out, pack: pack)
    }
}
