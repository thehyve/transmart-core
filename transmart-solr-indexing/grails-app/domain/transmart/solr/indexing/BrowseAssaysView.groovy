package transmart.solr.indexing

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseAssaysView implements Serializable {

    String id
    String title
    String description
    String measurementType
    String platformName
    String vendor
    String technology
    String gene
    String mirna
    String biomarkerType

    static mapping = {
        table schema: 'biomart_user'
        version false

        id         insert: false, update: false
    }

    static constraints = {
        id      nullable: true, maxSize: 300
        title           nullable: true, maxSize: 1000
        description     nullable: true, maxSize: 2000
        measurementType nullable: true
        platformName    nullable: true
        vendor          nullable: true
        technology      nullable: true
        gene            nullable: true
        mirna           nullable: true
        biomarkerType   nullable: true
    }

    def getIdentifier() {
        id
    }
}
