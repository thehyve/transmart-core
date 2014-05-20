package org.transmartproject.db.dataquery.highdim.vcf

class DeVariantSubjectIdxCoreDb {
    Long id
    DeVariantDatasetCoreDb dataset
    String subjectId
    Long position

    static mapping = {
        table schema: 'deapp', name: 'de_variant_subject_idx'
        version false
        id column:'variant_subject_idx_id'
        id generator:'sequence', params:[sequence:'de_variant_subject_idx_seq', schema: 'deapp']

        dataset column:'dataset_id'
        subjectId column:'subject_id'
        position column:'position'
    }

}
