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

package org.transmartproject.db.dataquery.highdim.rnaseq

import grails.gorm.CriteriaBuilder
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

class RnaSeqValuesProjection implements CriteriaProjection<RnaSeqValues>, MultiValueProjection {

    Map<String, Class> dataProperties = RnaSeqValues.metaClass.properties.collectEntries {
        it.name != 'class' ? [(it.name): it.type] : [:]
    }

    @Override
    void doWithCriteriaBuilder(CriteriaBuilder builder) {
        // RnaSeqModule.prepareDataQuery already projects all the columns
    }

    @Override
    RnaSeqValues doWithResult(Object object) {
        new RnaSeqValuesImpl(data: object)
    }

    class RnaSeqValuesImpl implements RnaSeqValues {

        def data

        Long getAssayId() { data.assayId as Long }

        Integer getReadcount() { data.readcount as Integer }

        Double getNormalizedReadcount() { data.normalizedReadcount as Double }

        Double getLogNormalizedReadcount() { data.logNormalizedReadcount as Double }

        Double getZscore() { data.zscore as Double }
    }
}
