package org.transmartproject.db.search

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = [ 'domainObjectId', 'associatedBioMarkerId', 'correlationType', 'valueMetric', 'mvId' ])
class SearchBioMarkerCorrelationView implements Serializable {

    //is a view!

	Long domainObjectId
	Long associatedBioMarkerId
	String correlationType
	Long valueMetric
	Long mvId

	static mapping = {
        table                 schema:    'searchapp', name: 'search_bio_mkr_correl_view'

        id                    composite: ['domainObjectId',   'associatedBioMarkerId', 'correlationType', 'valueMetric', 'mvId']

        associatedBioMarkerId column:    'asso_bio_marker_id'
        correlationType       column:    'correl_type'

        version               false
	}

	static constraints = {
        domainObjectId        nullable: true
        associatedBioMarkerId nullable: true
        correlationType       nullable: true, maxSize: 19
        valueMetric           nullable: true
        mvId                  nullable: true
	}
}
