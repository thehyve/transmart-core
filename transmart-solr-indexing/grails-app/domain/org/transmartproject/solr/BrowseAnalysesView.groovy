package org.transmartproject.solr

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseAnalysesView implements Serializable {

    String id
    String title
    String description
    String measurementType
    String platformName
    String vendor
    String technology

    static mapping = {
        table schema: 'biomart_user'
        version false

        id         insert: false, update: false
    }

    static constraints = {
        id              nullable: true, maxSize: 300
        title           nullable: true, maxSize: 500
        description     nullable: true, maxSize: 4000
        measurementType nullable: true
        platformName    nullable: true
        vendor          nullable: true
        technology      nullable: true
    }

    def getIdentifier() {
        id
    }
}
