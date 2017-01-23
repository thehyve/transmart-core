/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.ontology

import grails.orm.HibernateCriteriaBuilder
import grails.util.Holders
import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.concept.ConceptKey

@EqualsAndHashCode(includes = [ 'tableCode' ])
class TableAccess extends AbstractQuerySpecifyingType implements
        OntologyTerm, Serializable {

    Integer      level
    String       fullName
    String       name
    String       code

    String       tableName

    String       tableCode
    Character    cProtectedAccess
    Character    cSynonymCd = 'N'
    String       cVisualattributes = ''

    BigDecimal   cTotalnum
    String       cMetadataxml
    String       cComment
    String       tooltip
    Date         cEntryDate
    Date         cChangeDate
    Character    cStatusCd
    String       valuetypeCd

	static mapping = {
        table   name: 'table_access', schema: 'I2B2METADATA'
		version false

        /* hibernate needs an id, see
         * http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/mapping.html#mapping-declaration-id
         */
        id          composite: ['tableCode']

        fullName             column:   'C_FULLNAME'
        level                column:   'C_HLEVEL'
        name                 column:   'C_NAME'
        code                 column:   'C_BASECODE'
        tooltip              column:   'C_TOOLTIP'
        tableName            column:   'C_TABLE_NAME'
        tableCode            column:   'C_TABLE_CD'

        factTableColumn      column:   'C_FACTTABLECOLUMN'
        dimensionTableName   column:   'C_DIMTABLENAME'
        columnName           column:   'C_COLUMNNAME'
        columnDataType       column:   'C_COLUMNDATATYPE'
        operator             column:   'C_OPERATOR'
        dimensionCode        column:   'C_DIMCODE'
	}

	static constraints = {
        tableCode           maxSize:    50
        tableName           maxSize:    50
        cProtectedAccess    nullable:   true
        fullName            maxSize:    700
        name                maxSize:    2000
        cSynonymCd          nullable:   true
        cVisualattributes   maxSize:    3
        cTotalnum           nullable:   true
        code                nullable:   true,   maxSize:   50
        cMetadataxml        nullable:   true
        cComment            nullable:   true
        tooltip             nullable:   true,   maxSize:   900
        cEntryDate          nullable:   true
        cChangeDate         nullable:   true
        cStatusCd           nullable:   true
        valuetypeCd         nullable:   true,   maxSize:   50

        AbstractQuerySpecifyingType.constraints.delegate = delegate
        AbstractQuerySpecifyingType.constraints()
	}

    static List<OntologyTerm> getCategories(showHidden = false,
                                            showSynonyms = false) {
        withCriteria {
            if (!showHidden) {
                not { like 'cVisualattributes', '_H%' }
            }
            if (!showSynonyms) {
                eq 'cSynonymCd', 'N' as char
            }
        }
    }

    Class getOntologyTermDomainClassReferred()
    {
        def domainClass = Holders.getGrailsApplication().domainClasses.find
                {
                    AbstractI2b2Metadata.class.isAssignableFrom(it.clazz) &&
                            tableName.equalsIgnoreCase(it.clazz.backingTable)
                }
        domainClass?.clazz
    }

    ConceptKey getConceptKey() {
        new ConceptKey(tableCode, fullName)
    }

    @Override
    String getKey() {
        conceptKey.toString()
    }

    @Override
    EnumSet<VisualAttributes> getVisualAttributes() {
        VisualAttributes.forSequence(cVisualattributes)
    }

    @Override
    Object getMetadata() {
        null /* no metadata on categories supported */
    }

    boolean isSynonym() {
        cSynonymCd != 'Y'
    }

    @Override
    List<OntologyTerm> getChildren(boolean showHidden = false,
                                   boolean showSynonyms = false) {

        getDescendants(false, showHidden, showSynonyms)
    }

    @Override
    List<OntologyTerm> getAllDescendants(boolean showHidden = false,
                                         boolean showSynonyms = false) {
        getDescendants(true, showHidden, showSynonyms)
    }

    private List<OntologyTerm> getDescendants(boolean allDescendants,
                                              boolean showHidden = false,
                                              boolean showSynonyms = false) {

        HibernateCriteriaBuilder c

        /* extract table code from concept key and resolve it to a table name */
        c = TableAccess.createCriteria()
        String tableName = c.get {
            projections {
                distinct('tableName')
            }
            eq('tableCode', this.conceptKey.tableCode)
        }

        /* validate this table name */
        def domainClass = this.ontologyTermDomainClassReferred
        if (!domainClass) {
            throw new RuntimeException("Metadata table ${tableName} is not " +
                    "mapped")
        }

        /* select level on the original table (is this really necessary?) */
        c = domainClass.createCriteria();
        Integer parentLevel = c.get {
            projections {
                property 'level'
            }

            and {
                eq 'fullName', fullName
                eq 'cSynonymCd', 'N' as char
            }
        }
        if (parentLevel == null)
            throw new RuntimeException("Could not determine parent's level; " +
                    "could not find it in ${domainClass}'s table (fullname: " +
                    "$fullName)")

        /* Finally select the relevant stuff */
        def fullNameSearch = fullName.asLikeLiteral() + '%'

        c = domainClass.createCriteria()
        c.list {
            and {
                like 'fullName', fullNameSearch
                if (allDescendants) {
                    gt 'level', parentLevel
                } else {
                    eq 'level', parentLevel + 1
                }

                if (!showHidden) {
                    not { like 'cVisualattributes', '_H%' }
                }
                if (!showSynonyms) {
                    eq 'cSynonymCd', 'N' as char
                }
            }
            order('name')
        }
    }

    @Override
    Study getStudy() {
        /* never has an associated tranSMART study;
         * in tranSMART table access will only have 'Public Studies' and
         * 'Private Studies' nodes */
        null
    }

    @Override
    List<Patient> getPatients() {
        return super.getPatients(this)
    }

    @Override
    String toString() {
        getClass().canonicalName + "[${attached?'attached':'not attached'}" +
                "] [ fullName=$fullName ]"
    }
}
