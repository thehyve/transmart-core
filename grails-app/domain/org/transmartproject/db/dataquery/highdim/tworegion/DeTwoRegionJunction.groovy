package org.transmartproject.db.dataquery.highdim.tworegion

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.highdim.tworegion.Junction
import org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping

/**
 * Created by j.hudecek on 4-12-2014.
 */
@EqualsAndHashCode()
class DeTwoRegionJunction implements Serializable, Junction {

    Long upEnd
    String upChromosome
    Long upPos
    Character upStrand
    Long downEnd
    String downChromosome
    Long downPos
    Character downStrand
    Boolean isInFrame
    DeSubjectSampleMapping assay

    Boolean isInFrame() {
        isInFrame
    }

    static hasMany = [junctionEvents: DeTwoRegionJunctionEvent]

    static constraints = {
        junctionEvents(nullable: true)
        upChromosome(maxSize: 50)
        downChromosome(maxSize: 50)
        upStrand(nullable: true)
        downStrand(nullable: true)
        isInFrame(nullable: true)
    }

    static mapping = {
        table schema: 'deapp', name: 'de_two_region_junction'
        version false
        id column: 'two_region_junction_id'

        upEnd column: 'up_end'
        upPos column: 'up_pos'
        upChromosome column: 'up_chr'
        upStrand column: 'up_strand', sqlType: "char", length: 1
        downEnd column: 'down_end'
        downPos column: 'down_pos'
        downChromosome column: 'down_chr'
        downStrand column: 'down_strand', sqlType: "char", length: 1
        isInFrame column: 'is_in_frame'

        /* references */
        assay column: 'assay_id'

        version false
    }


    public HashMap<String, Object> toMap() {
        def ret = new HashMap<String, Object>()
        ret.put('id', id)
        ret.put('upEnd', upEnd)
        ret.put('upChromosome', upChromosome)
        ret.put('upPos', upPos)
        ret.put('upStrand', upStrand)
        ret.put('downEnd', downEnd)
        ret.put('downChromosome', downChromosome)
        ret.put('downPos', downPos)
        ret.put('downStrand', downStrand)
        ret.put('isInFrame', isInFrame)
        ret
    }
}
