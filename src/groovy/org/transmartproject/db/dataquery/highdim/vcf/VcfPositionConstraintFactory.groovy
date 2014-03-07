package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.db.dataquery.highdim.chromoregion.ChromosomeSegmentConstraintFactory

/**
 * Created by j.hudecek on 7-3-14.
 */
class VcfPositionConstraintFactory extends ChromosomeSegmentConstraintFactory {
    String regionPrefix = 'jDetail.'
    String segmentChromosomeColumn = 'chr'
    String segmentStartColumn      = 'pos'
    String segmentEndColumn        = 'pos'
}
