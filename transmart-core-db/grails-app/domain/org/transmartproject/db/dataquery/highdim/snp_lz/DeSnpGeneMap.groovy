package org.transmartproject.db.dataquery.highdim.snp_lz

class DeSnpGeneMap {

    String snpName
    Long entrezGeneId
    String geneName

    static mapping = {
        table           schema: 'deapp',    name:      'de_snp_gene_map'
        id              column: 'snp_id',   generator: 'assigned'
        snpName         column: 'snp_name'
        entrezGeneId    column: 'entrez_gene_id'
        geneName        column: 'gene_name'

        version false
    }

    static constraints = {
        snpName         nullable: true, maxSize: 255
        entrezGeneId    nullable: true
        geneName        nullable: true, maxSize: 255
    }
}
