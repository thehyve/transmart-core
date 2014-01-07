package org.transmartproject.db.bioassay

import groovy.transform.EqualsAndHashCode
import org.transmartproject.db.biomarker.BioMarkerCoreDb

@EqualsAndHashCode(includes = [ 'bioMarker', 'probeSet' ])
class BioAssayDataAnnotationCoreDb implements Serializable {
    String dataTable

    static belongsTo = [
            bioMarker: BioMarkerCoreDb,
            probeSet:  BioAssayFeatureGroupCoreDb,
    ]

    static mapping = {
        table schema: 'biomart', name: 'bio_assay_data_annotation'

        id composite: [ 'bioMarker', 'probeSet' ]

        bioMarker column: 'bio_marker_id'
        probeSet  column: 'bio_assay_feature_group_id'

        version false
    }

    static constraints = {
        dataTable nullable: true
    }
}
