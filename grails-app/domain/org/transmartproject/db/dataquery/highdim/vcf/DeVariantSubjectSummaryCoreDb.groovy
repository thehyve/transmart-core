package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

class DeVariantSubjectSummaryCoreDb {

    static final Integer REF_ALLELE = 0
    String subjectId
    String rsId
    String variant
    String variantFormat
    String variantType
    Boolean reference
    Integer allele1
    Integer allele2

    DeVariantSubjectDetailCoreDb jDetail

    static belongsTo = [dataset: DeVariantDatasetCoreDb, assay: DeSubjectSampleMapping]   //TODO: implement constraint on dataset



    static constraints = {
        variant(nullable: true)
        variantFormat(nullable: true)
        variantType(nullable: true)
    }

    static mapping = {
        table schema: 'deapp'
        table   name:  'de_variant_subject_summary'
        version false
        id  column:'variant_subject_summary_id',
            generator: 'sequence',
            params: [sequence: 'de_variant_subject_summary_seq', schema: 'deapp']
        dataset column: 'dataset_id'
        assay   column: 'assay_id'
        // this is needed due to a Criteria bug.
        // see https://forum.hibernate.org/viewtopic.php?f=1&t=1012372
        columns {
            jDetail {
                column name: 'chr'
                column name: 'pos'
            }
        }
    }
}
