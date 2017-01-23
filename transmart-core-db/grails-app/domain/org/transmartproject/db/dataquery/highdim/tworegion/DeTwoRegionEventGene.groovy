package org.transmartproject.db.dataquery.highdim.tworegion

import groovy.transform.EqualsAndHashCode
import org.transmartproject.core.dataquery.highdim.tworegion.EventGene

/**
 * Created by j.hudecek on 7-1-2015.
 */
@EqualsAndHashCode()
class DeTwoRegionEventGene implements Serializable, EventGene {

    String geneId
    String effect
    DeTwoRegionEvent event

    Long getEventId() {
        return event.id
    }

    static constraints = {
        geneId(nullable: true, maxSize: 50)
        effect(nullable: true, maxSize: 500)
    }

    static mapping = {
        table schema: 'deapp', name: 'de_two_region_event_gene'
        version false
        id column: 'two_region_event_gene_id'

        geneId column: 'gene_id'
        effect column: 'effect'
        /* references */
        event column: 'event_id'
        event fetch: 'join'
    }
}

