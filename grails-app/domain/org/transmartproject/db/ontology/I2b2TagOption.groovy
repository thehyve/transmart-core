package org.transmartproject.db.ontology

class I2b2TagOption {

    String value

    static  mapping = {
        table           name: 'i2b2_tag_options', schema: 'i2b2metadata'
        version         false

        id              column: 'tag_option_id', generator: 'sequence'
    }

}
