package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTermTag

class I2b2Tag implements OntologyTermTag {

    String ontologyTermFullName
    String name
    String description
    Long position

    static  mapping = {
        table                   name: 'i2b2_tags', schema: 'i2b2metadata'
        version                 false

        id                      column: 'tag_id', generator: 'sequence', params: [sequence: 'seq_i2b2_data_id']
        ontologyTermFullName    column: 'path'
        name                    column: 'tag_type'
        description             column: 'tag'
        position                column: 'tags_idx'
        sort                    'position'
    }

}
