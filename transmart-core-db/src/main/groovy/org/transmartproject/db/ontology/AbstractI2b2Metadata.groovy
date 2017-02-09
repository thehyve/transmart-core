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
import groovy.transform.EqualsAndHashCode
import org.hibernate.criterion.MatchMode
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.OntologyTerm.VisualAttributes
import org.transmartproject.core.ontology.Study
import org.transmartproject.core.concept.ConceptKey
import org.transmartproject.db.util.StringUtils
import org.transmartproject.db.i2b2data.PatientDimension

import static org.transmartproject.db.util.StringUtils.asLikeLiteral

@EqualsAndHashCode(includes = [ 'fullName', 'name' ])
abstract class AbstractI2b2Metadata extends AbstractQuerySpecifyingType
        implements OntologyTerm {

    Integer      level
    String       fullName
    String       name
    /**
     * Please do not rely on the 'basecode' for any references to concepts,
     * the {@link #dimensionCode} field is for that (if {@link #dimensionTableName}
     * ~= 'concept_dimension').
     */
    @Deprecated
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
        fullName           column: 'C_FULLNAME'
        level              column: 'C_HLEVEL'
        name               column: 'C_NAME'
        code               column: 'C_BASECODE'
        tooltip            column: 'C_TOOLTIP'
        factTableColumn    column: 'C_FACTTABLECOLUMN'
        dimensionTableName column: 'C_TABLENAME'
        columnName         column: 'C_COLUMNNAME'
        columnDataType     column: 'C_COLUMNDATATYPE'
        operator           column: 'C_OPERATOR'
        dimensionCode      column: 'C_DIMCODE'
        metadataxml        column: 'C_METADATAXML'
    }

    static constraints = {
        level             nullable: false, min:     0
        fullName          nullable: false, size:    2..700
        name              nullable: false, size:    1..2000
        code              nullable: true,  maxSize: 50
        tooltip           nullable: true,  maxSize: 900
        cVisualattributes nullable: false, size:    1..3
        cSynonymCd        nullable: false
        metadataxml       nullable: true

        AbstractQuerySpecifyingType.constraints.delegate = delegate
        AbstractQuerySpecifyingType.constraints()
    }

    @Override
    EnumSet<VisualAttributes> getVisualAttributes() {
        VisualAttributes.forSequence(cVisualattributes)
    }

    boolean isSynonym() {
        cSynonymCd == 'Y'
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

        def seriesMeta = slurper.SeriesMeta
        if (seriesMeta) {
            ret.seriesMeta = [
                    "unit": seriesMeta.Unit?.toString(),
                    "value": seriesMeta.Value?.toString(),
                    "label": seriesMeta.DisplayName?.toString(),
            ]
        }
        ret
    }

    @Override
    Study getStudy() {
        // since Study (in this sense) is a transmart concept, this only makes
        // sense for objects from tranSMART's i2b2 metadata table: I2b2
        null
    }

    @Override
    OntologyTerm getParent() {
        HibernateCriteriaBuilder c
        def fullNameSearch = this.conceptKey.conceptFullName.parent.toString()

        c = createCriteria()
        List<AbstractI2b2Metadata> ret = c.list {
            and {
                eq 'fullName', fullNameSearch
                eq 'level', level - 1
            }
        }
        if (!ret || ret.empty) {
            return null
        }
        def parentNode = ret[0]
        parentNode.setTableCode(getTableCode())
        parentNode
    }

    @Override
    List<OntologyTerm> getChildren(boolean showHidden = false,
                                   boolean showSynonyms = false) {

        getDescendants(false, showHidden, showSynonyms)
    }

    //@Override
    List<OntologyTerm> getAllDescendants(boolean showHidden = false,
                                         boolean showSynonyms = false) {
        getDescendants(true, showHidden, showSynonyms)
    }

	@Override
	List<OntologyTerm> getHDforAllDescendants() {
		getHDDescendants(true, false, false, false)
	}

    //@Override
    List<String> getAllDescendantsForFacets() {
        getDescendantsFullName(true, false, false, false)
    }

	private List<OntologyTerm> getHDDescendants(boolean allDescendants,
			boolean showHidden = false,
			boolean showSynonyms = false,
			boolean isOrdered = true) {
		HibernateCriteriaBuilder c
		def fullNameSearch = this.conceptKey.conceptFullName.toString()

        c = createCriteria()
		def ret = c.list {
			and {
				like 'fullName', fullNameSearch
				if (allDescendants) {
					gt 'level', level
				} else {
					eq 'level', level + 1
				}

				if (!showHidden) {
					not { like 'cVisualattributes', '_H%' }
				}
				if (!showSynonyms) {
					eq 'cSynonymCd', 'N' as char
				}
				like 'cVisualattributes', '__H%'
			}
			if (isOrdered) {
                order('name')
            }
		}
		ret.each { it.setTableCode(getTableCode()) }
		ret
	}

    private List<OntologyTerm> getDescendants(boolean allDescendants,
                                              boolean showHidden = false,
                                              boolean showSynonyms = false,
											  boolean isOrdered = true) {
        HibernateCriteriaBuilder c
        def fullNameSearch =  this.conceptKey.conceptFullName.toString()

        c = createCriteria()
        def ret = c.list {
            and {
                add(StringUtils.like('fullName', fullNameSearch, MatchMode.START))
                if (allDescendants) {
                    gt 'level', level
                } else {
                    eq 'level', level + 1
                }

                if (!showHidden) {
                    not { like 'cVisualattributes', '_H%' }
                }
                if (!showSynonyms) {
                    eq 'cSynonymCd', 'N' as char
                }
            }
			if (isOrdered) {
                order('name')
            }
        }
        ret.each { it.setTableCode(getTableCode()) }
        ret
    }

	private List<String> getDescendantsFullName(boolean allDescendants,
												  boolean showHidden = false,
												  boolean showSynonyms = false,
												  boolean isOrdered = true) {
			HibernateCriteriaBuilder c
			def fullNameSearch = asLikeLiteral(this.conceptKey.conceptFullName.toString()) + '%'

			c = createCriteria()
			def ret = c.list {
				projections {
					property('fullName')
				}
				and {
					like 'fullName', fullNameSearch
					if (allDescendants) {
						gt 'level', level
					} else {
						eq 'level', level + 1
					}

					if (!showHidden) {
						not { like 'cVisualattributes', '_H%' }
					}
					if (!showSynonyms) {
						eq 'cSynonymCd', 'N' as char
					}
				}
				if (isOrdered)
				order('name')
			}
			ret.each { it.setTableCode(getTableCode()) }
			ret
		}

    @Override
    List<Patient> getPatients() {
        super.getPatients(this)
    }

    @Override
    int getPatientCount() {
        super.countPatients(this)
    }

    @Override
    String toString() {
        getClass().canonicalName + "[${attached?'attached':'not attached'}" +
                "] [ fullName=$fullName, level=$level,  ]"
    }
}
