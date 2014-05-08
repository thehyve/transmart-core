package org.transmartproject.db.dataquery.highdim.vcf

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode()
class DeVariantSubjectDetailCoreDb implements Serializable {

    String chr
    Long   pos
    String rsId
    String ref
    String alt
    String quality
    String filter
    String info
    String format
    String variant

    static belongsTo = [dataset: DeVariantDatasetCoreDb] //TODO: implement constraint on dataset

    static constraints = {
        alt     nullable: true
        quality nullable: true
        filter  nullable: true
        info    nullable: true
        format  nullable: true
        variant nullable: true
    }

    static mapping = {
        table     schema:  'deapp', name: 'de_variant_subject_detail'
        version   false

        id composite: ['chr', 'pos']

        dataset column: 'dataset_id'
        rsId    column: 'rs_id'
        ref     column: 'ref'
        alt     column: 'alt'
        quality column: 'qual'
        filter  column: 'filter'
        info    column: 'info'
        format  column: 'format'
        variant column: 'variant_value', sqlType: 'clob'
    }

}
