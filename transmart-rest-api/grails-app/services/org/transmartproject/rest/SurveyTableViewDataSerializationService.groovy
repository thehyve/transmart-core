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
import org.transmartproject.core.multidimquery.MultiDimConstraint
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.users.User
import org.transmartproject.db.multidimquery.SurveyTableView
import org.transmartproject.rest.serialization.DataSerializer
import org.transmartproject.rest.serialization.Format
import org.transmartproject.rest.serialization.tabular.TabularResultSPSSSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultSerializer
import org.transmartproject.rest.serialization.tabular.TabularResultTSVSerializer

import java.util.zip.ZipOutputStream

@Transactional
class SurveyTableViewDataSerializationService implements DataSerializer {

    @Autowired
    MultiDimensionalDataResource multiDimService

    Map<Format, TabularResultSerializer> formatToSerializer = [
            (Format.TSV) : new TabularResultTSVSerializer(),
            (Format.SPSS): new TabularResultSPSSSerializer()
    ]
            .withDefault { Format format -> throw new UnsupportedOperationException("Unsupported format for tabular data: ${format}") }

    @Override
    void writeClinical(Format format,
                       MultiDimConstraint constraint,
                       User user,
                       OutputStream out) {
        def patientDimension = multiDimService.getDimension('patient')
        def hypercube = multiDimService.retrieveClinicalData(constraint, user, [patientDimension])
        def tabularView = new SurveyTableView(hypercube)
        try {
            log.info "Writing tabular data in ${format} format."
            formatToSerializer[format].writeFilesToZip(user, tabularView, (ZipOutputStream) out)
        } finally {
            tabularView.close()
        }
    }

    @Override
    void writeHighdim(Format format,
                      String type,
                      MultiDimConstraint assayConstraint,
                      MultiDimConstraint biomarkerConstraint,
                      String projection,
                      User user,
                      OutputStream out) {
        throw new UnsupportedOperationException("Writing HD data for this view is not supported.")
    }

    @Override
    Set<Format> getSupportedFormats() {
        formatToSerializer.keySet()
    }

}
