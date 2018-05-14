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
package org.transmartproject.rest.serialization

import org.transmartproject.core.dataquery.PaginationParameters
import org.transmartproject.core.dataquery.TableConfig
import org.transmartproject.core.multidimquery.DataRetrievalParameters
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.users.User

interface DataSerializer {

    /**
     * Write clinical data to the output stream
     *
     * @param format
     * @param constraint
     * @param user The user accessing the data
     * @param out
     * @param options
     */
    void writeClinical(Format format,
                       DataRetrievalParameters parameters,
                       User user,
                       OutputStream out)

    /**
     * Write tabular data to the output stream.
     *
     * @param format
     * @param constraint
     * @param tableConfig
     * @param user
     * @param out
     */
    void writeTable(Format format,
                    Constraint constraint,
                    TableConfig tableConfig,
                    User user,
                    OutputStream out)

    /**
     * Write tabular data page to the output stream.
     *
     * @param format
     * @param constraint
     * @param tableConfig
     * @param pagination
     * @param user
     * @param out
     */
    void writeTablePage(Format format,
                    Constraint constraint,
                    TableConfig tableConfig,
                    PaginationParameters pagination,
                    User user,
                    OutputStream out)

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
                      OutputStream out)

    /**
     * @return Supported file formats by this implementation.
     */
    Set<Format> getSupportedFormats()

}
