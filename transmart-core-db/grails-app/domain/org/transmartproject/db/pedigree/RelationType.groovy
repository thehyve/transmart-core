/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.pedigree

class RelationType {

    String label
    String description
    Boolean symmetrical
    Boolean biological

    static mapping = {
        table schema: 'I2B2DEMODATA'
        id generator: 'sequence', params: [sequence: 'relation_type_id_seq']
        version false
    }

    static constraints = {
        label maxSize: 200, nullable: false, unique: true
        description nullable: true
        symmetrical nullable: true
        biological nullable: true
    }
}
