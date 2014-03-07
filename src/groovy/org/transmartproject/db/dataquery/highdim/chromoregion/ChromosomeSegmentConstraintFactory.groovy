package org.transmartproject.db.dataquery.highdim.chromoregion

import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.parameterproducers.AbstractMethodBasedParameterFactory
import org.transmartproject.db.dataquery.highdim.parameterproducers.ProducerFor

import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.convertToLong
import static org.transmartproject.db.dataquery.highdim.parameterproducers.BindingUtils.getParam

@Component
class ChromosomeSegmentConstraintFactory extends AbstractMethodBasedParameterFactory {

    String regionPrefix = 'region.'
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
            regionPrefix = regionPrefix
            regionChromosomeColumn = segmentChromosomeColumn
            regionStartColumn = segmentStartColumn
            regionEndColumn = segmentEndColumn
            it
        })
        return chr
    }

}
