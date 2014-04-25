package org.transmartproject.db.dataquery.highdim

import org.hibernate.cfg.NotYetImplementedException
import org.transmartproject.core.dataquery.highdim.Platform

class DeGplInfo implements Platform {

    String  id
    String  title
    String  organism
    Date    annotationDate
    String  markerType
    String  genomeReleaseId

    static mapping = {
        table         schema: 'deapp'

        id              column: 'platform',   generator: 'assigned'
        genomeReleaseId column: 'release_nbr'

        version      false
    }

    static constraints = {
        id             maxSize:  50

        title           nullable: true, maxSize: 500
        organism        nullable: true, maxSize: 100
        annotationDate  nullable: true
        markerType      nullable: true, maxSize: 100
        genomeReleaseId nullable: true
    }

    @Override
    Iterable<?> getTemplate() {
        throw new NotYetImplementedException()
    }
}
