package org.transmartproject.db.dataquery.highdim.vcf

import org.springframework.stereotype.Component
import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory

/**
 * Created by j.hudecek on 7-3-14.
 */
@Component
class VcfPositionConstraintFactory extends ChromosomeSegmentConstraintFactory {
    String segmentPrefix = 'jDetail.'
    String segmentChromosomeColumn = 'chr'
    String segmentStartColumn      = 'pos'
    String segmentEndColumn        = 'pos'
}
