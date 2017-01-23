package org.transmartproject.db.dataquery.highdim.tworegion

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.highdim.tworegion.Event

/**
 * Created by j.hudecek on 4-12-2014.
 */
@EqualsAndHashCode()
class DeTwoRegionEvent implements Serializable, Event {

    String cgaType
    String soapClass

    static hasMany = [eventGenes: DeTwoRegionEventGene]

    static constraints = {
        cgaType(nullable: true, maxSize: 500)
        soapClass(nullable: true, maxSize: 500)
        eventGenes(nullable: true)
    }

    static mapping = {
        table schema: 'deapp', name: 'de_two_region_event'
        version false
        id column: 'two_region_event_id'
        eventGenes fetch: 'join'

        cgaType column: 'cga_type'
        soapClass column: 'soap_class'

    }
}
