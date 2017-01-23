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
        new AcghValuesImpl(data: object)
    }

    class AcghValuesImpl implements AcghValues {

        def data

        Long getAssayId() { data.assayId as Long }

        Double getChipCopyNumberValue() { data.chipCopyNumberValue as Double }

        Double getSegmentCopyNumberValue() { data.segmentCopyNumberValue as Double }

        CopyNumberState getCopyNumberState() { CopyNumberState.forInteger((data.flag as Short).intValue()) }

        Double getProbabilityOfLoss() { data.probabilityOfLoss as Double }

        Double getProbabilityOfNormal() { data.probabilityOfNormal as Double }

        Double getProbabilityOfGain() { data.probabilityOfGain as Double }

        Double getProbabilityOfAmplification() { data.probabilityOfAmplification as Double }
    }
}
