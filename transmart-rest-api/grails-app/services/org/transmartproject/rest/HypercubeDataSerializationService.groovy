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
import org.transmartproject.core.multidimquery.*
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.AbstractDataResourceService
import org.transmartproject.rest.serialization.*
import org.transmartproject.core.multidimquery.export.Format

@Transactional
class HypercubeDataSerializationService extends AbstractDataResourceService {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    PatientSetResource patientSetService

    Map<Format, HypercubeSerializer> formatToSerializer = [
            (Format.JSON)    : new HypercubeJsonSerializer(),
            (Format.PROTOBUF): new HypercubeProtobufSerializer()
    ]
            .withDefault { Format format -> throw new UnsupportedOperationException("Unsupported format: ${format}") }

    /**
     * Write clinical data to the output stream
     *
     * @param format
     * @param parameters
     * @param user The user accessing the data
     * @param out
     * @param options
     */
    void writeClinical(Format format,
                       DataRetrievalParameters parameters,
                       User user,
                       OutputStream out) {
        checkAccess(parameters.constraint, user, PatientDataAccessLevel.MEASUREMENTS)
        Hypercube hypercube = multiDimService.retrieveClinicalData(parameters, user)

        try {
            log.info "Writing to format: ${format}"
            formatToSerializer[format].write(hypercube, out, dataType: 'clinical')
        } finally {
            hypercube.close()
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
                      Constraint assayConstraint,
                      Constraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out) {
        checkAccess(assayConstraint, user, PatientDataAccessLevel.MEASUREMENTS)
        Hypercube hypercube = multiDimService.highDimension(assayConstraint, biomarkerConstraint, projection, user, type)

        try {
            log.info "Writing to format: ${format}"
            formatToSerializer[format].write(hypercube, out, dataType: type)
        } finally {
            hypercube.close()
        }
    }

}
