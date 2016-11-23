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

package org.transmartproject.db.dataquery.highdim.correlations

import groovy.transform.EqualsAndHashCode

/**
 * Associations between some objects (generally bio markers, but not
 * necessarily) and bio markers. Note that these 'correlations' are actual
 * unidirectional.
 *
 * Most of the correlation types are actually listed in the database in
 * <code>BIO_DATA_CORREL_DESCR<code>, though not all (namely those in
 * <code>SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW</code>); additionally, it
 * does not seem expressive enough for our purposes.
 *
 * Mere container; has no behavior.
 */
@EqualsAndHashCode(includes = [ 'name' ])
class CorrelationType {

    /**
     * The name of the association, as found in
     * BIO_MARKER_CORREL_MV.CORREL_MV or similar
     */
    String name

    /**
     * The type of the object of the left side of the association, as seen on
     * the <code>DATA_CATEGORY</code> column of
     * <code>SEARCHAPP.SEARCH_KEYWORD</code> (and typically also on the
     * <code>BIO_MARKER_TYPE</code> column of <code>BIOMART.BIO_MARKER</code>.
     */
    String sourceType

    /**
     * The type of the bio marker on the right side of the association, i.e.,
     * the bio marker type of the bio marker pointed to in the
     * ASSO_BIO_MARKER_ID column of the <code>correlationTable</code> table.
     */
    String targetType

    /**
     * The table where the associations are stored. By default, this is the
     * <code>BIOMART.BIO_MARKER_CORREL_MV</code> view (actually implemented as
     * materialized view or even a fake materialized view -- a table that's
     * manually updated from time to time -- in some tranSMART branches).
     *
     * The view <code>SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW</code> is the
     * table to used for associations related with 'gene signatures'.
     */
    String correlationTable = 'BIOMART.BIO_MARKER_CORREL_MV'

    /**
     * The column used to store the identifier of the object on the left side
     * of the association (on the <code>correlationTable</code>). By default,
     * this is BIO_MARKER_ID, which has the primary key of a bio marker object.
     *
     * For gene signatures, the column <code>DOMAIN_OBJECT_ID</code> from
     * <code>SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW</code> is used, which contains
     * the primary key of a gene signature object.
     */
    String leftSideColumn = 'BIO_MARKER_ID'

}
