package org.transmartproject.db.ontology

class TableAccess extends AbstractOntologyTerm implements Serializable {

    String       cTableCd
    String       cTableName
    Character    cProtectedAccess
    Integer         level
    String       fullName
    String       name
    Character    cSynonymCd
  //String       cVisualattributes /* defined in parent */
    BigDecimal   cTotalnum
    String       cBasecode
    String       cMetadataxml
    String       cFacttablecolumn
    String       cDimtablename
    String       cColumnname
    String       cColumndatatype
    String       cOperator
    String       cDimcode
    String       cComment
    String       tooltip
    Date         cEntryDate
    Date         cChangeDate
    Character    cStatusCd
    String       valuetypeCd

	static mapping = {
        table   'table_access'
		version false

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
        cTableCd            maxSize:    50
        cTableName          maxSize:    50
        cProtectedAccess    nullable:   true
        fullName            maxSize:    700
        name                maxSize:    2000
        cSynonymCd          nullable:   true
        cVisualattributes   maxSize:    3
        cTotalnum           nullable:   true
        cBasecode           nullable:   true,   maxSize:   50
        cMetadataxml        nullable:   true
        cFacttablecolumn    maxSize:    50
        cDimtablename       maxSize:    50
        cColumnname         maxSize:    50
        cColumndatatype     maxSize:    50
        cOperator           maxSize:    10
        cDimcode            maxSize:    700
        cComment            nullable:   true
        tooltip             nullable:   true,   maxSize:   900
        cEntryDate          nullable:   true
        cChangeDate         nullable:   true
        cStatusCd           nullable:   true
        valuetypeCd         nullable:   true,   maxSize:   50

	}
}
