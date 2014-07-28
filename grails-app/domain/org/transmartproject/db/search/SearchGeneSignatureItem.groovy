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

package org.transmartproject.db.search

import org.transmartproject.db.bioassay.BioAssayFeatureGroupCoreDb
import org.transmartproject.db.biomarker.BioMarkerCoreDb

class SearchGeneSignatureItem {

    BioMarkerCoreDb            bioMarker
    Long                 foldChangeMetric
    String               bioDataUniqueId
    BioAssayFeatureGroupCoreDb probeSet

    static belongsTo = [
            geneSignature: SearchGeneSignature
    ]

	static mapping = {
        table schema: 'searchapp'

		id generator: 'assigned'

        geneSignature    column:  'search_gene_signature_id',
                         comment: 'associated gene signature'
        bioMarker        column:  'bio_marker_id',
                         comment: 'link to bio_marker table'
        foldChangeMetric column:  'fold_chg_metric'
                         comment: 'the corresponding fold change value metric ' +
                         '(actual number or -1,0,1 for composite gene signatures). ' +
                         'If null, it\'s assumed to be a gene list in which case all ' +
                         'genes are assumed to be up regulated'
        bioDataUniqueId  comment: 'link to unique_id from bio_data_uid table (context sensitive)'
        probeSet         column:  'bio_assay_feature_group_id'

		version false
	}

	static constraints = {
        bioMarker        nullable: true
        foldChangeMetric nullable: true
        bioDataUniqueId  nullable: true, maxSize: 200
        probeSet         nullable: true
	}
}
