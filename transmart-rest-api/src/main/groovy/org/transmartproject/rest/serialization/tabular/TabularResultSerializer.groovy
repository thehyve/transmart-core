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
package org.transmartproject.rest.serialization.tabular

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.multidimquery.StreamingDataTable
import org.transmartproject.core.users.User

import java.util.zip.ZipOutputStream

/**
 * Writes tabular data to an output.
 */
interface TabularResultSerializer {

    /**
     * Writes a tabular file content to the output stream.
     * Does not close the output stream afterwards.
     * Required to be thread-safe.
     *
     * @param tabularResult table which data to write.
     * @param task id of the task
     */
    void writeParallel(TabularResult tabularResult, int task)

    /**
     * Completes the writes, combines the data and writes it to the output stream.
     * Does not close the output stream afterwards.
     */
    void combine()

}
