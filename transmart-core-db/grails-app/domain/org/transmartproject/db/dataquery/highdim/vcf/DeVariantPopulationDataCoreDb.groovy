package org.transmartproject.db.dataquery.highdim.vcf

class DeVariantPopulationDataCoreDb {

    DeVariantDatasetCoreDb dataset
    String chromosome
    Long position
    String infoName
    Integer infoIndex
    Long integerValue
    Double floatValue
    String textValue

    static constraints = {
        infoName(nullable: true)
        infoIndex(nullable: true)
        integerValue(nullable: true)
        floatValue(nullable: true)
        textValue(nullable: true)
    }

    static mapping = {
        table schema: 'deapp', name: 'de_variant_population_data'
        version false

        id column:'variant_population_data_id', generator: 'sequence'

        dataset      column: 'dataset_id'
        chromosome   column: 'chr'
        position     column: 'pos'
        infoName     column: 'info_name'
        infoIndex    column: 'info_index'
        integerValue column: 'integer_value'
        floatValue   column: 'float_value'
        textValue    column: 'text_value'
    }
}
