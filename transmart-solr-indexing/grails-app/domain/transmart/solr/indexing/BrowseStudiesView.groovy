package transmart.solr.indexing

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseStudiesView implements Serializable {

    String id
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
        version false

        id         insert: false, update: false
    }

    static constraints = {
        id             nullable: true, maxSize: 300
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

    def getIdentifier() {
        id
    }
}
