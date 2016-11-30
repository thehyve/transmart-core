package org.transmartproject.db.dataquery.highdim.snp_lz

@Deprecated
class DeSnpInfo {

    String name
    String chromosome
    Long pos

    static mapping = {
        table      schema: 'deapp',       name:      'de_snp_info'
        id         column: 'snp_info_id', generator: 'assigned'
        chromosome column: 'chrom'
        pos        column: 'chrom_pos'

        version false
    }

    static constraints = {
        name       nullable: true, unique: true
        chromosome nullable: true, maxSize: 16
        pos        nullable: true
    }
}
