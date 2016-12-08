package org.transmartproject.db.ontology

class I2b2TagType {

    String tagType
    String solrFieldName
    String nodeType
    String valueType
    Boolean shownIfEmpty
    Integer index

    static hasMany = [options: I2b2TagOption]

    static  mapping = {
        table           name: 'i2b2_tag_types', schema: 'i2b2metadata'
        version         false

        id              column: 'tag_type_id', generator: 'sequence', params: [sequence: 'i2b2_tag_types_tag_type_id_seq']
        tagType         column: 'tag_type'
        solrFieldName   column: 'solr_field_name'
        nodeType        column: 'node_type'
        valueType       column: 'value_type'
        shownIfEmpty    column: 'shown_if_empty'
        index           column: '"index"'

        options         fetch: 'join', joinTable: [name: 'i2b2_tag_type_options', key: 'tag_type_id']

        sort            'index'
    }

}
