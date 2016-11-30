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

class BioAssayGenoPlatformProbe {

    String origChrom
    BigDecimal origPosition
    String origGenomeBuild
    String probeName

    // boolean represented as char in Oracle
    // probably needs an extra transient property to support both oracle's char
    // and a bona fide boolean in postgres. Other tables generally use NUMBER(1,0)
    // in Oracle for PostgreSQL's booleans.
    //Character isControl

    String createdBy
    String modifiedBy
    Date dateCreated
    Date lastUpdated

    static belongsTo = [
        bioAssayPlatform: CoreBioAssayPlatform,
    ]

    static mapping = {
        table               schema:  'biomart'
        id                  column:  'bio_asy_geno_platform_probe_id',  generator:  'assigned'
        bioAssayPlatform    column:  'bio_assay_platform_id',  generator: 'assigned'
        dateCreated         column:  'created_date'
        lastUpdated         column:  'modified_date'
        version             false
    }

    static constraints = {
        origChrom        nullable:  true,  maxSize:  5
        origPosition     nullable:  true
        origGenomeBuild  nullable:  true,  maxSize:  20
        probeName        nullable:  true,  maxSize:  200

        //isControl      nullable:  true,  maxSize:  1

        bioAssayPlatform nullable:  true

        createdBy        nullable:  true,  maxSize:  30
        dateCreated      nullable:  true
        modifiedBy       nullable:  true,  maxSize:  30
        lastUpdated      nullable:  true
    }
}
