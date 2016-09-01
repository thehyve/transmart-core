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

import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.Study

class I2b2 extends AbstractI2b2Metadata implements Serializable {

    BigDecimal   cTotalnum
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

    static transients = AbstractI2b2Metadata.transients + ['studyId', 'study']

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

    String getStudyId() {
        def matcher = cComment =~ /(?<=^trial:).+/
        if (matcher.find()) {
            matcher.group 0
        }
    }

    @Override
    Study getStudy() {
        def trial = studyId

        if (!trial) {
            return null
        }

        def query = sessionFactory.currentSession.createQuery '''
                SELECT I
                FROM I2b2 I, I2b2TrialNodes TN
                WHERE (I.fullName = TN.fullName) AND
                TN.trial = :trial'''

        query.setParameter 'trial', trial

        def res = query.list()
        if (res.size() > 1) {
            throw new UnexpectedResultException("More than one study with name $trial")
        } else if (res.size() == 1) {
            new StudyImpl(ontologyTerm: res[0], id: trial)
        } else {
            null
        }
    }

}
