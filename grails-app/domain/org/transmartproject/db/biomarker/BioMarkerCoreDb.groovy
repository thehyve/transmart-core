package org.transmartproject.db.biomarker

class BioMarkerCoreDb {

    String name
    String description
    String organism
    String primarySourceCode
    String primaryExternalId
    String type

    static mapping = {
        table       name:   'bio_marker',    schema:    'biomart'
        id          column: 'bio_marker_id', generator: 'assigned'

        name        column: 'bio_marker_name'
        description column: 'bio_marker_description'
        type        column: 'bio_marker_type'

        version false
    }

    static constraints = {
        name              nullable: true, maxSize: 400
        description       nullable: true, maxSize: 2000
        organism          nullable: true, maxSize: 400
        primarySourceCode nullable: true, maxSize: 400
        primaryExternalId nullable: true, maxSize: 400
        type              maxSize:  400
    }
}
