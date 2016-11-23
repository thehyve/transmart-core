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

package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

class DeVariantSubjectSummaryCoreDb {

    String subjectId
    String rsId
    String variant
    String variantFormat
    String variantType
    Boolean reference
    Integer allele1
    Integer allele2
    String chr
    Long pos

    static belongsTo = [dataset: DeVariantDatasetCoreDb, assay: DeSubjectSampleMapping]
    //TODO: implement constraint on dataset

    static constraints = {
        variant(nullable: true)
        variantFormat(nullable: true)
        variantType(nullable: true)
    }

    static mapping = {
        table schema: 'deapp', name: 'de_variant_subject_summary'
        version false
        id column: 'variant_subject_summary_id',
                generator: 'sequence',
                params: [sequence: 'de_variant_subject_summary_seq', schema: 'deapp']

        dataset column: 'dataset_id'
        assay column: 'assay_id'
        subjectId column: 'subject_id'
    }
}
