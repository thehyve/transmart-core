package org.transmart.searchapp

import org.transmart.biomart.BioAnalysisAttribute;


public class BioAnalysisAttributeLineage  {
	Long id
	BioAnalysisAttribute bioAnalysisAttribute
    SearchTaxonomy ancestorTerm
	
	static mapping = {
		table 'BIO_ANALYSIS_ATTRIBUTE_LINEAGE'
		version false
		id column:'BIO_ANALYSIS_ATT_LINEAGE_ID'
		columns {
		   ancestorTerm column:'ANCESTOR_TERM_ID'
		   bioAnalysisAttribute column:'BIO_ANALYSIS_ATTRIBUTE_ID'
		}
	}
	

}

