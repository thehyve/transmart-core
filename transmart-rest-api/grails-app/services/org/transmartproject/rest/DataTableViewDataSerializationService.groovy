/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transmartproject.rest

import com.google.common.collect.ImmutableList
import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.multidimquery.DataTable
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User
import org.transmartproject.rest.serialization.DataSerializer
import org.transmartproject.rest.serialization.Format
import org.transmartproject.rest.serialization.tabular.TabularResultSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultTSVSerializer

import java.util.zip.ZipOutputStream

@Transactional
@CompileStatic
class DataTableViewDataSerializationService implements DataSerializer {

    @Autowired
    MultiDimensionalDataResource multiDimService

    Set<Format> supportedFormats = [Format.TSV] as Set<Format>

    TabularResultSerializer getSerializer(Format format, User user, ZipOutputStream zipOutputStream, ImmutableList<DataColumn> columns) {
        switch(format) {
            case Format.TSV:
                return new TabularResultTSVSerializer(user, zipOutputStream, columns)
            default:
                throw new UnsupportedOperationException("Unsupported format for tabular data: ${format}")
        }
    }

    /**
     * Write clinical data to the output stream.
     *
     * @param format
     * @param constraint
     * @param user
     * @param out
     * @param options
     */
    @Override
    void writeClinical(Format format,
                       Constraint constraint,
                       User user,
                       OutputStream out,
                       Map options) {

        def tableArgs = options.tableConfig
        Boolean includeMeasurementDateColumns = options.includeMeasurementDateColumns

        //TODO include measurement date columns, similar as below
//        final ImmutableList<MetadataAwareDataColumn> columns
//        if (includeMeasurementDateColumns == null) {
//            columns = surveyTableColumnService
//                    .getMetadataAwareColumns(dataTableColumns)
//        } else {
//            columns = surveyTableColumnService
//                    .getMetadataAwareColumns(dataTableColumns, includeMeasurementDateColumns)
//        }

        //TODO fix tableArgs parsing (rowDimensions, columnDimensions, rowSort, columnSort)
        StreamingDataTable datatable = multiDimService.retrieveStreamingDataTable(
                (Map)tableArgs, 'clinical', constraint, user)
        try {
            log.info "Writing tabular data in ${format} format."
            TabularResultSerializer serializer = getSerializer(format, user, (ZipOutputStream) out,
                    null)
            //serializer.writeFilesToZip(user, datatable, (ZipOutputStream) out)
        } finally {
            datatable.close()
            log.info "Writing tabular data in ${format} format completed."
        }
    }

    @Override
    void writeHighdim(Format format,
                      String type,
                      Constraint assayConstraint,
                      Constraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out) {
        throw new UnsupportedOperationException("Writing HD data for this view is not supported.")
    }

}
