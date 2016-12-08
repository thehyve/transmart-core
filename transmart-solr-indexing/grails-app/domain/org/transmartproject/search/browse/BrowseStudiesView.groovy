package org.transmartproject.search.browse

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseStudiesView implements Serializable {

    String identifier
    String title
    String description
    String design
    String biomarkerType
    String accessType
    String accession
    String institution
    String country
    String disease
    String compound
    String studyObjective
    String organism
    String studyPhase

    static mapping = {
        table schema: 'biomart_user'
        id composite: ['identifier']
        version false

        id         insert: false, update: false
        identifier column: 'id'
    }

    static constraints = {
        identifier     nullable: true, maxSize: 300
        title          nullable: true, maxSize: 1000
        description    nullable: true, maxSize: 4000
        design         nullable: true, maxSize: 2000
        biomarkerType  nullable: true
        accessType     nullable: true, maxSize: 100
        accession      nullable: true, maxSize: 100
        institution    nullable: true, maxSize: 400
        country        nullable: true, maxSize: 1000
        disease        nullable: true
        compound       nullable: true
        studyObjective nullable: true
        organism       nullable: true
        studyPhase     nullable: true
    }
}
