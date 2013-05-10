package org.transmartproject.db.highdim

import org.hibernate.cfg.NotYetImplementedException
import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.PlatformMarkerType

class DeGplInfo implements Platform {

    String id
    String title
    String organism
    Date   annotationDate
    String markerTypeId
    String genomeBuild

    static transients = ['markerType']

	static mapping = {
        table        schema: 'deapp'

        id           column: "platform",   generator: "assigned"
        markerTypeId column: 'marker_type'

        version      false
	}

	static constraints = {
        id             maxSize:  50

        title          nullable: true, maxSize: 500
        organism       nullable: true, maxSize: 100
        annotationDate nullable: true
        markerTypeId   nullable: true, maxSize: 100
        genomeBuild    nullable: true, maxSize: 20
	}

    @Override
    PlatformMarkerType getMarkerType() {
        PlatformMarkerType.forId(markerTypeId)
    }

    @Override
    Iterable<?> getTemplate() {
        throw new NotYetImplementedException()
    }
}
