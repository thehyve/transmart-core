/*
 * Copyright © 2013-2015 The Hyve B.V.
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

package org.transmartproject.db.dataquery.highdim.snp_lz

import groovy.transform.InheritConstructors
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl

/**
 * Extends {@link HighDimensionDataTypeResourceImpl} to open stateful sessions,
 * since Hibernate's StatelessSession cannot retrieve LOBs.
 */
@InheritConstructors
class SnpLzHighDimensionDataTypeResource extends  HighDimensionDataTypeResourceImpl {

    @Override
    protected StatelessSession openSession() {
        SessionFactory sessionFactory = module.sessionFactory
        StatelessSession statelessSession = sessionFactory.openStatelessSession()
        //To prevent out of memory exception for big data sets: fetch data from the database in the cursor mode.
        statelessSession.connection().autoCommit = false
        statelessSession
    }
}
