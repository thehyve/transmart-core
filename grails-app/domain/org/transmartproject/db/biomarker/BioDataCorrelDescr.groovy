package org.transmartproject.db.biomarker

class BioDataCorrelDescr {

    String correlation /* name */
    String description
    String typeName
    String status
    String source
    String sourceCode

    static hasMany = [ correlationRows: BioDataCorrelationCoreDb ]

    static mapping = {
        table   name:   'bio_data_correl_descr',    schema:    'biomart'
        id      column: 'bio_data_correl_descr_id', generator: 'assigned'
        version false
    }

    static constraints = {
        correlation nullable: true, maxSize: 1020
        description nullable: true, maxSize: 2000
        typeName    nullable: true, maxSize: 400
        status      nullable: true, maxSize: 400
        source      nullable: true, maxSize: 200
        sourceCode  nullable: true, maxSize: 400
    }
}
