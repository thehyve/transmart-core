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

        geneSignature    column:  'search_gene_signature_id'
        bioMarker        column:  'bio_marker_id'
        foldChangeMetric column:  'fold_chg_metric'
        bioDataUniqueId  column: 'bio_data_unique_id'
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
