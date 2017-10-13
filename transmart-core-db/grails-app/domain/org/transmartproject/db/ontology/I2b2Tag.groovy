/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.ontology

import org.transmartproject.core.ontology.OntologyTermTag

class I2b2Tag implements OntologyTermTag {

    String ontologyTermFullName
    String name
    String tag
    Long position
    I2b2TagOption option

    static  mapping = {
        table                   name: 'i2b2_tags', schema: 'i2b2metadata'
        version                 false

        id                      column: 'tag_id', generator: 'sequence', params: [sequence: 'seq_i2b2_data_id']
        ontologyTermFullName    column: 'path'
        name                    column: 'tag_type'
        tag                     column: 'tag'
        position                column: 'tags_idx'
        option                  column: 'tag_option_id'

        sort                    'position'
    }

    static constraints = {
        tag     nullable: true
        option  nullable: true
    }

    /**
     * Return the description (the free text field) if not null;
     * the option value (selected from a fixed vocabulary) otherwise.
     *
     * @return the tag value.
     */
    String getDescription() {
        tag ?: option?.value
    }

}
