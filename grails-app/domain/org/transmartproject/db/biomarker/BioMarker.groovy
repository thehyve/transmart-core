package org.transmartproject.db.biomarker

class BioMarker {

	String bioMarkerName
	String bioMarkerDescription
	String organism
	String primarySourceCode
	String primaryExternalId
	String bioMarkerType

	static mapping = {
        table   name:   'bio_marker',    schema:    'biomart'
        id      column: 'bio_marker_id', generator: 'assigned'
        version false
	}

	static constraints = {
        bioMarkerName        nullable: true, maxSize: 400
        bioMarkerDescription nullable: true, maxSize: 2000
        organism             nullable: true, maxSize: 400
        primarySourceCode    nullable: true, maxSize: 400
        primaryExternalId    nullable: true, maxSize: 400
        bioMarkerType        maxSize:  400
	}
}
