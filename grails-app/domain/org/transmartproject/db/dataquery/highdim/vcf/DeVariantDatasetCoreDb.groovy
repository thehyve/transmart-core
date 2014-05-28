package org.transmartproject.db.dataquery.highdim.vcf

class DeVariantDatasetCoreDb {

    String id
    String datasourceId
    String etlId
    Date etlDate
    String genome
    String metadataComment
    String variantDatasetType

    static hasMany = [summaries: DeVariantSubjectSummaryCoreDb, details: DeVariantSubjectDetailCoreDb]

    static constraints = {
        datasourceId(nullable: true)
        etlId(nullable: true)
        etlDate(nullable: true)
        metadataComment(nullable: true)
        variantDatasetType(nullable: true)
    }

    static mapping = {
        table schema: 'deapp', name:  'de_variant_dataset'
        version false
        id column:'dataset_id', generator: 'assigned'
    }
}
