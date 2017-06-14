/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.rest.serialization.HypercubeCSVSerializer
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer
import org.transmartproject.rest.serialization.HypercubeSerializer
import org.transmartproject.rest.serialization.HypercubeJsonSerializer

@Transactional
class MultidimensionalDataService {

    @Autowired
    MultiDimensionalDataResource multiDimService

    /**
     * Type to represent the requested serialization format.
     */
    static enum Format {
        JSON('application/json'),
        PROTOBUF('application/x-protobuf'),
        TSV('TSV'),
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
    @CompileStatic
    private void serialise(Map args, Hypercube hypercube, Format format, OutputStream out) {
        HypercubeSerializer serializer
        switch (format) {
            case Format.JSON:
                serializer = new HypercubeJsonSerializer()
                break
            case Format.PROTOBUF:
                serializer = new HypercubeProtobufSerializer()
                break
            case Format.TSV:
                serializer = new HypercubeCSVSerializer()
                break
            default:
                throw new Exception("Unsupported format: ${format}")
        }
        serializer.write(args, hypercube, out)
    }

    /**
     * Write clinical data to the output stream
     *
     * @param format
     * @param constraint
     * @param user The user accessing the data
     * @param out
     */
    void writeClinical(Map args, Format format, MultiDimConstraint constraint, User user, OutputStream out) {

        Hypercube result = multiDimService.retrieveClinicalData(constraint, user)

        try {
            log.info "Writing to format: ${format}"
            serialise(args, result, format, out)
        } finally {
            result.close()
        }
    }

    /**
     * Write high dimensional data to the output stream
     *
     * @param format
     * @param type The type of highdim data or 'autodetect'
     * @param assayConstraint
     * @param biomarkerConstraint
     * @param projection
     * @param user
     * @param out
     */
    void writeHighdim(Format format,
                      String type,
                      MultiDimConstraint assayConstraint,
                      MultiDimConstraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out) {

        Hypercube hypercube = multiDimService.highDimension(assayConstraint, biomarkerConstraint, projection, user, type)
        Map args = format == Format.TSV ? [dataType : type] : [:]

        try {
            log.info "Writing to format: ${format}"
            serialise(args, hypercube, format, out)
        } finally {
            hypercube.close()
        }

    }
}
