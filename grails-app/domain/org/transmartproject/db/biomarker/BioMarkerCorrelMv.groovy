package org.transmartproject.db.biomarker

class BioMarkerCorrelMv implements Serializable {

    /* view in PostgreSQL, table in Oracle (sanofi) */

	String correlationType
    Long   mvId

    static belongsTo = [
            bioMarker:           BioMarker, /* maybe these can come from other tables */
            associatedBioMarker: BioMarker,
    ]

	static mapping = {
        table               schema:    'biomart'

        id                  composite: [ 'bioMarker', 'associatedBioMarker' ]

        bioMarker           column:    'bio_marker_id'
        associatedBioMarker column:    'asso_bio_marker_id'
        correlationType     column:    'correl_type'

		version false
	}

	static constraints = {
        bioMarker           nullable: false /* actually true in db */
        associatedBioMarker nullable: false /* idem */
        correlationType     nullable: true, maxSize: 15
        mvId                nullable: true
	}
}
