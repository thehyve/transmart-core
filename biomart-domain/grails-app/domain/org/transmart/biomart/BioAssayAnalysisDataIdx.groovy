package org.transmart.biomart

import com.recomdata.util.IExcelProfile

class BioAssayAnalysisDataIdx implements IExcelProfile {
		
	Long id
	String ext_type
	String field_name
	Integer field_idx
	Integer display_idx
		
	static mapping = {
	 table name:'BIO_ASY_ANALYSIS_DATA_IDX', schema:'BIOMART'
	 version false
	 id generator:'sequence', params:[sequence:'SEQ_BIO_DATA_ID']
	 columns {
		id column:'BIO_ASY_ANALYSIS_DATA_IDX_ID'
		ext_type column:'EXT_TYPE'
		field_name column:'FIELD_NAME'
		field_idx column:'FIELD_IDX'
		display_idx column:'DISPLAY_IDX'
		}
	}

	/**
	 * Get values to Export to Excel
	 */
	public List getValues() {
		return [ext_type,field_name,field_idx,display_idx]
	}
}