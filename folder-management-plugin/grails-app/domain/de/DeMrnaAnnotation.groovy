package de

class DeMrnaAnnotation implements Serializable {
    String gplId
    String probeId
    String geneSymbol
    Long probesetId
    Long geneId
    String organism

    static mapping = {
        table 'DE_MRNA_ANNOTATION'
        version false
        id column: 'DE_MRNA_ANNOTATION_ID'
        columns {
            gplId column: 'GPL_ID'
            probeId column: 'PROBE_ID'
            geneSymbol column: 'GENE_SYMBOL'
            probesetId column: 'PROBESET_ID'
            geneId column: 'GENE_ID'
            organism column: 'ORGANISM'
        }
    }
    static constraints = {
        geneSymbol(nullable: true)
        geneId(nullable: true)
    }
}
