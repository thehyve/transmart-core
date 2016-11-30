/*
 * Copyright © 2016 The Hyve B.V.
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

class CoreBioAssayPlatform {

    String name
    String version
    String description
    String array
    String accession
    String organism
    String vendor
    String type
    String technology
    String imputedAlgorithm
    String imputedPanel
    String imputed

    String createdBy
    String modifiedBy
    Date dateCreated
    Date lastUpdated

    static mapping = {
        table               name: 'bio_assay_platform',  schema:  'biomart'
        id                  column:  'bio_assay_platform_id',  generator:  'assigned'

        name                column:  'platform_name'
        version             column:  'platform_version'
        description         column:  'platform_description'
        array               column:  'platform_array'
        accession           column:  'platform_accession'
        organism            column:  'platform_organism'
        vendor              column:  'platform_vendor'
        type                column:  'platform_type'
        technology          column:  'platform_technology'
        imputedAlgorithm    column:  'platform_imputed_algorithm'
        imputedPanel        column:  'platform_imputed_panel'
        imputed             column:  'platform_imputed'

        dateCreated         column:  'created_date'
        lastUpdated         column:  'modified_date'
        version             false
    }

    static constraints = {
        name                nullable:  true,  maxSize:  200
        version             nullable:  true,  maxSize:  200
        description         nullable:  true,  maxSize:  2000
        array               nullable:  true,  maxSize:  50
        accession           nullable:  true,  maxSize:  20
        organism            nullable:  true,  maxSize:  200
        vendor              nullable:  true,  maxSize:  200
        type                nullable:  true,  maxSize:  200
        technology          nullable:  true,  maxSize:  200
        imputedAlgorithm    nullable:  true,  maxSize:  500
        imputedPanel        nullable:  true,  maxSize:  200
        imputed             nullable:  true,  maxSize:  1
    
        createdBy           nullable:  true,  maxSize:  30
        dateCreated         nullable:  true
        modifiedBy          nullable:  true,  maxSize:  30
        lastUpdated         nullable:  true
    }
}
