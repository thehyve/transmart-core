/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest

import grails.core.GrailsApplication
import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.SubjectObservationsByStudyConceptsTableView
import org.transmartproject.rest.serialization.HypercubeCSVSerializer
import org.transmartproject.rest.serialization.HypercubeJsonSerializer
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer
import org.transmartproject.rest.serialization.HypercubeSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultTSVSerializer

import java.util.zip.ZipOutputStream

@Transactional
class MultidimensionalDataService {

    @Autowired
    MultiDimensionalDataResource multiDimService
    @Autowired
    GrailsApplication grailsApplication

    /**
     * Type to represent the requested serialization format.
     */
    static enum Format {
        JSON('application/json'),
        PROTOBUF('application/x-protobuf'),
        TSV('TSV'),
        SPSS('SPSS'),
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
                throw new UnsupportedOperationException("Unsupported format: ${format}")
        }
        serializer.write(args, hypercube, out)
    }

    /**
     * Serialises tabular data to <code>out</code>.
     *
     * @param tabularResult the table to serialise.
     * @param out the stream to serialise to.
     */
    @CompileStatic
    private void serialise(TabularResult tabularResult, Format format, OutputStream out) {
        switch (format) {
            case Format.TSV:
                TabularResultTSVSerializer.writeFilesToZip(tabularResult, (ZipOutputStream) out)
                break
            case Format.SPSS:
                TabularResultSPSSSerializer.writeFilesToZip(tabularResult, (ZipOutputStream) out)
                break
            default:
                throw new UnsupportedOperationException("Unsupported format for tabular data: ${format}")
        }
    }

    /**
     * Write clinical data to the output stream
     *
     * @param format
     * @param constraint
     * @param user The user accessing the data
     * @param out
     * @param view predefined string that specifies how exported data structure will look like
     */
    void writeClinical(Format format,
                       MultiDimConstraint constraint,
                       User user,
                       OutputStream out,
                       String view = null) {

        String dataView = view ?: grailsApplication.config.export.clinical.defaultDataView

        if (!dataView) {
            Hypercube result = multiDimService.retrieveClinicalData(constraint, user)

            try {
                log.info "Writing to format: ${format}"
                serialise(result, format, out, dataType: 'clinical')
            } finally {
                result.close()
            }
        } else if (dataView == 'subjectObservationsByStudyConceptsTableView') {
            def patientDimension = multiDimService.getDimension('patient')
            def hypercube = multiDimService.retrieveClinicalData(constraint, user, [patientDimension])
            def customizations = grailsApplication.config
                    .export.clinical.subjectObservationsByStudyConceptsTableView
            def tabularView = new SubjectObservationsByStudyConceptsTableView(customizations, hypercube)
            try {
                log.info "Writing tabular data in ${format} format."
                serialise(tabularView, format, out)
            } finally {
                tabularView.close()
            }
        } else {
            throw new UnsupportedOperationException("${dataView} data view is not supported.")
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
     * @param view predefined string that specifies how exported data structure will look like
     */
    void writeHighdim(Format format,
                      String type,
                      MultiDimConstraint assayConstraint,
                      MultiDimConstraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out,
                      String view = null) {

        if (view) {
            throw new UnsupportedOperationException("${view} data view is not supported.")
        }

        Hypercube hypercube = multiDimService.highDimension(assayConstraint, biomarkerConstraint, projection, user, type)

        try {
            log.info "Writing to format: ${format}"
            serialise(hypercube, format, out, dataType: type)
        } finally {
            hypercube.close()
        }

    }
}
