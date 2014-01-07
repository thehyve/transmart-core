package org.transmartproject.db.search

class SearchKeywordCoreDb {

	String keyword
    /* usage patterns shows joins of bioDataId with:
     * search_bio_mkr_correl_fast_mv.domain_object_id
     * search_bio_mkr_correl_view.domain_object_id
     * bio_marker_correl_mv.bio_marker_id
     */
	Long   bioDataId
	String uniqueId            /* for genes: GENE: primary_external_id (in bio_marker) */
	String dataCategory
	String displayDataCategory

    // Do not map this point (though they exist in the database):
    //String sourceCode
	//BigDecimal ownerAuthUserId

	static mapping = {
        table   schema: 'searchapp',         name:     'search_keyword'
		id      column: 'search_keyword_id', generator: 'assigned'
		version false
	}

	static constraints = {
        keyword             nullable: true,  maxSize: 400
        bioDataId           nullable: true
        uniqueId            maxSize:  1000
        dataCategory        maxSize:  400,   unique: 'uniqueId'
        displayDataCategory nullable: true,  maxSize: 400

        // see above:
        //sourceCode        nullable: true,  maxSize: 200
        //ownerAuthUserId   nullable: true
	}
}
