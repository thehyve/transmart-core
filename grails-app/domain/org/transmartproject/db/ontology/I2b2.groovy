package org.transmartproject.db.ontology

import javax.persistence.PrePersist
import javax.persistence.PreUpdate

class I2b2 extends AbstractOntologyTerm implements Serializable {

    //String       cVisualattributes /* defined in parent */

    Integer      level
    String       fullName
    String       name
    Character    cSynonymCd = 'N'
    BigDecimal   cTotalnum
    String       cBasecode
    String       cMetadataxml
    String       cFacttablecolumn
    String       cTablename
    String       cColumnname
    String       cColumndatatype
    String       cOperator
    String       cDimcode
    String       cComment
    String       tooltip
    String       mAppliedPath
    Date         updateDate
    Date         downloadDate
    Date         importDate
    String       sourcesystemCd
    String       valuetypeCd
    String       mExclusionCd
    String       cPath
    String       cSymbol

    static mapping = {
        table         'i2b2'
        version       false

        /* hibernate needs an id, see
         * http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/mapping.html#mapping-declaration-id
         */
        id          composite: ['fullName', 'name']

        fullName    column: 'C_FULLNAME'
        level       column: 'C_HLEVEL'
        name        column: 'C_NAME'
        tooltip     column: 'C_TOOLTIP'
	}

	static constraints = {
        level               nullable:   false,   min:       0
        fullName            nullable:   false,   size:      2..700
        name                nullable:   false,   size:      1..2000
        cVisualattributes   nullable:   false,   size:      1..3
        cSynonymCd          nullable:   false
        cTotalnum           nullable:   true
        cBasecode           nullable:   true,    maxSize:   50
        cMetadataxml        nullable:   true
        cFacttablecolumn    nullable:   false,   maxSize:   50
        cTablename          nullable:   false,   maxSize:   150
        cColumnname         nullable:   false,   maxSize:   50
        cColumndatatype     nullable:   false,   maxSize:   50
        cOperator           nullable:   false,   maxSize:   10
        cDimcode            nullable:   false,   maxSize:   700
        cComment            nullable:   true
        tooltip             nullable:   true,    maxSize:   900
        mAppliedPath        nullable:   false,   maxSize:   700
        downloadDate        nullable:   true
        updateDate          nullable:   false
        importDate          nullable:   true
        sourcesystemCd      nullable:   true,    maxSize:   50
        valuetypeCd         nullable:   true,    maxSize:   50
        mExclusionCd        nullable:   true,    maxSize:   25
        cPath               nullable:   true,    maxSize:   700
        cSymbol             nullable:   true,    maxSize:   50
	}

}
