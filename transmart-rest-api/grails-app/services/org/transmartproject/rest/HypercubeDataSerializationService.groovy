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

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.rest.serialization.*

@Transactional
class HypercubeDataSerializationService implements DataSerializer {

    @Autowired
    MultiDimensionalDataResource multiDimService

    Map<Format, HypercubeSerializer> formatToSerializer = [
            (Format.JSON)    : new HypercubeJsonSerializer(),
            (Format.PROTOBUF): new HypercubeProtobufSerializer(),
            (Format.TSV)     : new HypercubeCSVSerializer(),
    ]
            .withDefault { Format format -> throw new UnsupportedOperationException("Unsupported format: ${format}") }

    @Override
    void writeClinical(Format format,
                       Constraint constraint,
                       User user,
                       OutputStream out,
                       Map options = [:]) {

        Hypercube hypercube = multiDimService.retrieveClinicalData(options, constraint, user)

        try {
            log.info "Writing to format: ${format}"
            formatToSerializer[format].write(hypercube, out, dataType: 'clinical')
        } finally {
            hypercube.close()
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
        Hypercube hypercube = multiDimService.highDimension(assayConstraint, biomarkerConstraint, projection, user, type)

        try {
            log.info "Writing to format: ${format}"
            formatToSerializer[format].write(hypercube, out, dataType: type)
        } finally {
            hypercube.close()
        }
    }

    void writeTable(Constraint constraint, List<String> rowDimensions, List<String> columnDimensions, sort,
                    User user, OutputStream out) {
        def datatable = multiDimService.retrieveDataTable('clinical', constraint, user,
                rowDimensions: rowDimensions, columnDimensions: columnDimensions, sort: sort)

        DataTableSerializer.write(datatable, out, userSorting: multiDimService.parseSort(sort))
    }

    @Override
    Set<Format> getSupportedFormats() {
        formatToSerializer.keySet()
    }
}
