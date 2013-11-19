package org.transmartproject.db.search

import org.transmartproject.db.bioassay.BioAssayFeatureGroup
import org.transmartproject.db.biomarker.BioMarker

class SearchGeneSignatureItem {

    BioMarker            bioMarker
    Long                 foldChangeMetric
    String               bioDataUniqueId
    BioAssayFeatureGroup probeSet

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
