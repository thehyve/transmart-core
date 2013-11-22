package org.transmartproject.db.bioassay

import org.transmartproject.db.biomarker.BioMarkerCoreDb

class BioAssayFeatureGroupCoreDb implements Serializable {
    String name
    String type

    static hasMany = [ markers: BioMarkerCoreDb ]

    static mapping = {
        table schema: 'biomart', name: 'bio_assay_feature_group'

        id column: 'bio_assay_feature_group_id', generator:'assigned'

        name column: 'feature_group_name'
        type column: 'feature_group_type'

        markers joinTable: [ name: 'biomart.bio_assay_data_annotation', key: 'bio_assay_feature_group_id' ]

        version false
    }

    static constraints = {
        name maxSize: 100
        type maxSize: 50
    }
}
