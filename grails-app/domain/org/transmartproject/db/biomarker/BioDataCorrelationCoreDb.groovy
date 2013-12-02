package org.transmartproject.db.biomarker

class BioDataCorrelationCoreDb {


    /* unidirectional relationship between
      the left bio marker and the right bio marker */
    static belongsTo = [ description:    BioDataCorrelDescr,
                         leftBioMarker:  BioMarkerCoreDb,
                         rightBioMarker: BioMarkerCoreDb ]

    static mapping = {
        table          name:   'bio_data_correlation',     schema:    'biomart'

        id             column: 'bio_data_correl_id',       generator: 'assigned'
        description    column: 'bio_data_correl_descr_id', cascade:   'save-update'
        leftBioMarker  column: 'bio_data_id'
        rightBioMarker column: 'asso_bio_data_id'

        version        false
    }
}
