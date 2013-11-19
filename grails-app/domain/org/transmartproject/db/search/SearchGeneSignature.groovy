package org.transmartproject.db.search

import org.transmartproject.db.user.SearchAuthUser

class SearchGeneSignature {

    /* This is a very basic mapping
     * It's not supposed to support inserts, unlike the version in transmartApp
     */

    String              name
    String              description
    String              uniqueId
    Date                createDate
    SearchAuthUser      creator
    Date                lastModifiedDate
    SearchAuthUser      modifier
    String              versionNumber
    Boolean             publicFlag = false
    Boolean             deletedFlag = false
    SearchGeneSignature parentGeneSignature
    Long                sourceConceptId
    String              sourceOther
    Long                ownerConceptId
    String              stimulusDescription
    String              stimulusDosing
    String              treatmentDescription
    String              treatmentDosing
    Long                treatmentBioCompoundId
    String              treatmentProtocolNumber
    String              pmidList
    Long                speciesConceptId
    Long                speciesMouseSrcConceptId
    String              speciesMouseDetail
    Long                tissueTypeConceptId
    Long                experimentTypeConceptId
    String              experimentTypeInVivoDescr
    String              experimentTypeAtccRef
    Long                analyticCatConceptId
    String              analyticCatOther
    Long                bioAssayPlatformId
    String              analystName
    Long                normMethodConceptId
    String              normMethodOther
    Long                analysisMethodConceptId
    String              analysisMethodOther
    Boolean             multipleTestingCorrection
    Long                PValueCutoffConceptId
    String              uploadFile
    Long                searchGeneSigFileSchemaId //should actually reference search_gene_sig_file_schema
    Long                foldChgMetricConceptId
    Long                experimentTypeCellLineId


	static hasMany = [ searchGeneSignatures: SearchGeneSignature ]

	static mapping = {
        table schema: 'searchapp'

		id                      column: 'search_gene_signature_id',  generator: 'assigned'
        creator                 column: 'created_by_auth_user_id'
        modifier                column: 'modified_by_auth_user_id'

        parentGeneSignature     column: 'parent_gene_signature_id'
        'PValueCutoffConceptId' column: 'p_value_cutoff_concept_id'

		version false
	}

	static constraints = {
        name                      maxSize:  100
        description               nullable: true, maxSize: 1000
        uniqueId                  nullable: true, maxSize: 50
        lastModifiedDate          nullable: true
        modifier                  nullable: true
        versionNumber             nullable: true, maxSize: 50
        publicFlag                nullable: true
        deletedFlag               nullable: true
        sourceConceptId           nullable: true
        sourceOther               nullable: true
        ownerConceptId            nullable: true
        stimulusDescription       nullable: true, maxSize: 1000
        stimulusDosing            nullable: true
        treatmentDescription      nullable: true, maxSize: 1000
        treatmentDosing           nullable: true
        treatmentBioCompoundId    nullable: true
        treatmentProtocolNumber   nullable: true, maxSize: 50
        pmidList                  nullable: true
        speciesMouseSrcConceptId  nullable: true
        speciesMouseDetail        nullable: true
        tissueTypeConceptId       nullable: true
        experimentTypeConceptId   nullable: true
        experimentTypeInVivoDescr nullable: true
        experimentTypeAtccRef     nullable: true
        analyticCatConceptId      nullable: true
        analyticCatOther          nullable: true
        analystName               nullable: true, maxSize: 100
        normMethodConceptId       nullable: true
        normMethodOther           nullable: true
        analysisMethodConceptId   nullable: true
        analysisMethodOther       nullable: true
        multipleTestingCorrection nullable: true
        searchGeneSigFileSchemaId nullable: true
        foldChgMetricConceptId    nullable: true
        experimentTypeCellLineId  nullable: true
	}
}
