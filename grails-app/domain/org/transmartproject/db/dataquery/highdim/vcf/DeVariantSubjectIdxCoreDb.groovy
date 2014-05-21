package org.transmartproject.db.dataquery.highdim.vcf

class DeVariantSubjectIdxCoreDb implements Serializable {
    DeVariantDatasetCoreDb dataset
    String subjectId
    Long position

    static mapping = {
        table schema: 'deapp', name: 'de_variant_subject_idx'
        version false
        id composite: ['dataset', 'subjectId']

        dataset   column: 'dataset_id'
        subjectId column: 'subject_id'
        position  column: 'position'
    }

}
