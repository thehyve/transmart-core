package org.transmartproject.db.ontology

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes
import org.transmartproject.db.concept.ConceptKey

abstract class AbstractI2b2Metadata extends AbstractQuerySpecifyingType
        implements OntologyTerm {

    Integer      level
    String       fullName
    String       name
    String       code
    String       tooltip
    String       metadataxml

    /* properties abstracted with other properties */
    String       cVisualattributes = ''
    Character    cSynonymCd = 'N'

    /* Transient */
    String       tableCode

    static transients = [ 'synonym', 'metadata', 'tableCode' ]

    static mapping = {
        fullName             column:   'C_FULLNAME'
        level                column:   'C_HLEVEL'
        name                 column:   'C_NAME'
        code                 column:   'C_BASECODE'
        tooltip              column:   'C_TOOLTIP'
        factTableColumn      column:   'C_FACTTABLECOLUMN'
        dimensionTableName   column:   'C_TABLENAME'
        columnName           column:   'C_COLUMNNAME'
        columnDataType       column:   'C_COLUMNDATATYPE'
        operator             column:   'C_OPERATOR'
        dimensionCode        column:   'C_DIMCODE'
        metadataxml          column:   'C_METADATAXML'
    }

    static constraints = {
        level               nullable:   false,   min:       0
        fullName            nullable:   false,   size:      2..700
        name                nullable:   false,   size:      1..2000
        code                nullable:   true,    maxSize:   50
        tooltip             nullable:   true,    maxSize:   900
        cVisualattributes   nullable:   false,   size:      1..3
        cSynonymCd          nullable:   false
        metadataxml         nullable:   true

        AbstractQuerySpecifyingType.constraints.delegate = delegate
        AbstractQuerySpecifyingType.constraints()
    }

    @Override
    EnumSet<VisualAttributes> getVisualAttributes() {
        VisualAttributes.forSequence(cVisualattributes)
    }

    boolean isSynonym() {
        cSynonymCd != 'Y'
    }

    void setSynonym(boolean value) {
        cSynonymCd = value ? 'Y' : 'N'
    }

    private String getTableCode() {
        if (tableCode) {
            return tableCode
        }

        TableAccess candidate = null
        TableAccess.list().each {
            if (fullName.startsWith(it.fullName)) {
                if (!candidate ||
                        it.fullName.length() > candidate.fullName.length()) {
                    candidate = it
                }
            }
        }

        if (!candidate)
            throw new RuntimeException("Could not determine table code for " +
                    "$this")

        tableCode = candidate.tableCode
        tableCode
    }

    ConceptKey getConceptKey() {
        new ConceptKey(getTableCode(), fullName)
    }

    @Override
    String getKey() {
        conceptKey.toString()
    }

    @Override
    Object getMetadata() {
        if (!metadataxml)
            return null

        def slurper = new XmlSlurper().parseText(metadataxml)
        def ret = [:]

        /* right now we only care about normalunits and oktousevalues */
        ret.okToUseValues = slurper.Oktousevalues == 'Y' ? true : false
        ret.unitValues = [
                normalUnits: slurper.UnitValues?.NormalUnits?.toString(),
                equalUnits: slurper.UnitValues?.EqualUnits?.toString(),
        ]

        ret
    }

    @Override
    List<OntologyTerm> getChildren(boolean showHidden = false,
                                   boolean showSynonyms = false) {
        HibernateCriteriaBuilder c
        def fullNameSearch = this.conceptKey.conceptFullName.toString()
                .asLikeLiteral() + '%'

        c = createCriteria()
        def ret = c.list {
            and {
                like 'fullName', fullNameSearch
                eq 'level', level + 1
                if (!showHidden) {
                    not { like 'cVisualattributes', '_H%' }
                }
                if (!showSynonyms) {
                    eq 'cSynonymCd', 'N' as char
                }
            }
            order('name')
        }
        ret.each { it.setTableCode(getTableCode()) }
        ret
    }

    @Override
    String toString() {
        getClass().canonicalName + "[${attached?'attached':'not attached'}" +
                "] [ fullName=$fullName, level=$level,  ]"
    }
}
