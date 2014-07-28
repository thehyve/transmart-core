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

package org.transmartproject.db.dataquery.highdim.acgh

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.acgh.AcghValues
import org.transmartproject.core.dataquery.highdim.acgh.CopyNumberState
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

class AcghValuesProjection implements CriteriaProjection<AcghValues>, MultiValueProjection {

    Map<String, Class> dataProperties = AcghValues.metaClass.properties.collectEntries {
        it.name != 'class' ? [(it.name): it.type] : [:]
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        // AcghModule.prepareDataQuery already projects all the columns
    }

    @Override
    AcghValues doWithResult(Object object) {
        new AcghValuesImpl(object)
    }

    class AcghValuesImpl implements AcghValues {
        final List rowList

        AcghValuesImpl(final List rowList) {
            this.rowList = rowList
        }

        Long getAssayId() { rowList[0] as Long }

        Double getChipCopyNumberValue() { rowList[1] as Double }

        Double getSegmentCopyNumberValue() { rowList[2] as Double }

        CopyNumberState getCopyNumberState() { CopyNumberState.forInteger((rowList[3] as Short).intValue()) }

        Double getProbabilityOfLoss() { rowList[4] as Double }

        Double getProbabilityOfNormal() { rowList[5] as Double }

        Double getProbabilityOfGain() { rowList[6] as Double }

        Double getProbabilityOfAmplification() { rowList[7] as Double }
    }
}
