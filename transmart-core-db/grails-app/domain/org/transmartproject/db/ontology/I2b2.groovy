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

import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.util.GormWorkarounds

/**
 * Domain class for storing an ontology tree.
 * Redundant, the same data already resides in {@link I2b2Secure}.
 */
@Deprecated
class I2b2 extends AbstractI2b2Metadata implements Serializable {

    BigDecimal   cTotalnum
    /**
     * Do not use cComment to store study identifiers, use
     * it for comments instead.
     */
    String       cComment
    String       mAppliedPath
    Date         updateDate
    Date         downloadDate
    Date         importDate
    String       sourcesystemCd
    String       valuetypeCd
    String       mExclusionCd
    String       cPath
    String       cSymbol

    static String backingTable = 'I2B2'

    static transients = ['studyId', 'study', 'synonym', 'metadata', 'tableCode']

    static mapping = {
        table    name: 'I2B2', schema: 'I2B2METADATA'
        version  false

        /* hibernate needs an id, see
         * http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/mapping.html#mapping-declaration-id
         */
        id       composite: ['fullName', 'name']

        AbstractI2b2Metadata.mapping.delegate = delegate
        AbstractI2b2Metadata.mapping()
    }

    static constraints = {
        GormWorkarounds.fixupClassPropertyFetcher(I2b2)

        cTotalnum      nullable: true
        cComment       nullable: true
        mAppliedPath   nullable: false, maxSize: 700
        downloadDate   nullable: true
        updateDate     nullable: false
        importDate     nullable: true
        sourcesystemCd nullable: true,  maxSize: 50
        valuetypeCd    nullable: true,  maxSize: 50
        mExclusionCd   nullable: true,  maxSize: 25
        cPath          nullable: true,  maxSize: 700
        cSymbol        nullable: true,  maxSize: 50

        AbstractI2b2Metadata.constraints.delegate = delegate
        AbstractI2b2Metadata.constraints()
    }

    /**
     * Please do not use the comment field for storing study ids.
     */
    @Deprecated
    private String getStudyIdFromComment() {
        def matcher = cComment =~ /(?<=^trial:).+/
        if (matcher.find()) {
            return matcher.group(0)
        }
    }

    String getStudyId() {
        String studyIdFromComment = getStudyIdFromComment()
        if (studyIdFromComment) {
            return studyIdFromComment
        } else if (sourcesystemCd) {
            return sourcesystemCd
        } else {
            throw new RuntimeException('No study id found in the i2b2 table.')
        }
    }

    @Override
    Study getStudy() {
        if (OntologyTerm.VisualAttributes.STUDY in visualAttributes) {
            new StudyImpl(ontologyTerm: this, id: studyId)
        } else {
            parent?.study
        }
    }

}
