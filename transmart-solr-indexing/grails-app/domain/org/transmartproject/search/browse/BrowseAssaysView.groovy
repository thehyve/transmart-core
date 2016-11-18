package org.transmartproject.search.browse

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class BrowseAssaysView implements Serializable {

    String identifier
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
        id composite: ['identifier']
        version false

        id         insert: false, update: false
        identifier column: 'id'
    }

    static constraints = {
        identifier      nullable: true, maxSize: 300
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
}
