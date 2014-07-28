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

package org.transmartproject.db.dataquery.highdim.chromoregion

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.parameterproducers.AbstractMethodBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.ProducerFor

import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.convertToLong
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.getParam

@Component
@Scope("prototype")
class ChromosomeSegmentConstraintFactory extends AbstractMethodBasedParameterFactory {

    String segmentPrefix = 'region.'
    String segmentChromosomeColumn = 'chromosome'
    String segmentStartColumn      = 'start'
    String segmentEndColumn        = 'end'

    private static final String SEGMENT_CHROMOSOME_PARAM = 'chromosome'
    private static final String SEGMENT_START_PARAM      = 'start'
    private static final String SEGMENT_END_PARAM        = 'end'

    @ProducerFor(DataConstraint.CHROMOSOME_SEGMENT_CONSTRAINT)
    ChromosomeSegmentConstraint createChromosomeSegmentConstraint(Map<String, Object> params) {
        String chromosome
        Long   start,
               end

        if (params.isEmpty()) {
            throw new InvalidArgumentsException('This constraint requires ' +
                    'at least one parameter')
        }

        params.each { key, value ->
            if (key == SEGMENT_CHROMOSOME_PARAM) {
                chromosome = getParam  params, SEGMENT_CHROMOSOME_PARAM, String
                if (!chromosome) {
                    throw new InvalidArgumentsException(
                            'chromosome parameter cannot be empty')
                }
            } else if (key == SEGMENT_START_PARAM) {
                start = convertToLong SEGMENT_START_PARAM, value
            } else if (key == SEGMENT_END_PARAM) {
                end = convertToLong SEGMENT_END_PARAM, value
            } else {
                throw new InvalidArgumentsException("Unrecognized param: $key")
            }
        }

        if ((start == null && end != null) || (end == null && start != null)) {
            throw new InvalidArgumentsException('including the start parameter ' +
                    'makes the end parameter be required and vice-versa')
        }

        def chr = new ChromosomeSegmentConstraint(chromosome: chromosome, start: start, end: end).with({
            regionPrefix = segmentPrefix
            regionChromosomeColumn = segmentChromosomeColumn
            regionStartColumn = segmentStartColumn
            regionEndColumn = segmentEndColumn
            it
        })
        return chr
    }

}
