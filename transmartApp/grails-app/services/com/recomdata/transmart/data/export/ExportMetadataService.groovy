package com.recomdata.transmart.data.export

import grails.util.Holders
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.export.HighDimExporter
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.export.HighDimExporter

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.HIGH_DIMENSIONAL

class ExportMetadataService {

    static transactional = false

    def highDimensionResourceService
    def highDimExporterRegistry

    @Autowired
    QueriesResource queriesResource  // deprecated

    def Map createJSONFileObject(fileType, dataFormat, fileDataCount, gplId, gplTitle) {
        def file = [:]
        if(dataFormat!=null){
            file['dataFormat'] = dataFormat
        }
        if(fileType!=null){
            file['fileType'] = fileType
        }
        if(fileDataCount!=null){
            file['fileDataCount'] = fileDataCount
        }
        if(gplId!=null){
            file['gplId']=gplId
        }
        if(gplTitle!=null){
            file['gplTitle']=gplTitle
        }
        return file
    }

    def getMetaData(Long resultInstanceId1, Long resultInstanceId2) {
        def metadata = convertIntoMetaDataMap(
                getClinicalMetaData( resultInstanceId1, resultInstanceId2 ),
                getHighDimMetaData( resultInstanceId1, resultInstanceId2 )
        )

        metadata
    }


    def getClinicalMetaData(Long resultInstanceId1, Long resultInstanceId2 ) {
        //The result instance id's are stored queries which we can use to get information from the i2b2 schema.
        log.debug('rID1 :: ' + resultInstanceId1 + ' :: rID2 :: ' + resultInstanceId1)

        //Retrieve the counts for each subset.
        [
                subset1: resultInstanceId1 ? queriesResource.getQueryResultFromId( resultInstanceId1 ).getSetSize() : 0,
                subset2: resultInstanceId2 ? queriesResource.getQueryResultFromId( resultInstanceId2 ).getSetSize() : 0,
        ]
    }

    def getHighDimMetaData(Long resultInstanceId1, Long resultInstanceId2) {
        def (datatypes1, datatypes2) = [[:], [:]]

        if (resultInstanceId1) {
            def dataTypeConstraint = highDimensionResourceService.createAssayConstraint(
                    AssayConstraint.PATIENT_SET_CONSTRAINT,
                    result_instance_id: resultInstanceId1)

            datatypes1 = highDimensionResourceService.getSubResourcesAssayMultiMap([dataTypeConstraint])
        }

        if (resultInstanceId2) {
            def dataTypeConstraint = highDimensionResourceService.createAssayConstraint(
                    AssayConstraint.PATIENT_SET_CONSTRAINT,
                    result_instance_id: resultInstanceId2)

            datatypes2 = highDimensionResourceService.getSubResourcesAssayMultiMap([dataTypeConstraint])
        }

        // Determine the unique set of datatypes, for both subsets
        def uniqueDatatypes = ( datatypes1.keySet() + datatypes2.keySet() ).unique()

        // Combine the two subsets, into a map based on datatypes
        def hdMetaData = uniqueDatatypes.collect { datatype ->
            [
                    datatype: datatype,
                    subset1: datatypes1[ datatype ],
                    subset2: datatypes2[ datatype ]
            ]
        }

        hdMetaData
    }

    def getHighDimMetaData(OntologyTerm term) {

        // Retrieve all descendant terms that have the HIGH_DIMENSIONAL attribute
        def terms = term.getHDforAllDescendants() + term
        def highDimTerms = terms.findAll { it.visualAttributes.contains(HIGH_DIMENSIONAL) }

        if (highDimTerms) {
            // Put all high dimensional term keys in a disjunction constraint
            def constraint = highDimensionResourceService.createAssayConstraint(
                    AssayConstraint.DISJUNCTION_CONSTRAINT,
                    subconstraints: [
                            (AssayConstraint.ONTOLOGY_TERM_CONSTRAINT):
                                    highDimTerms.collect({
                                        [concept_key: it.key]
                                    })
                    ]
            )

            def datatypes = highDimensionResourceService.getSubResourcesAssayMultiMap([constraint])
            def dataTypeDescriptions = datatypes.keySet().collect({
                it.dataTypeDescription
            })

            [ dataTypes: dataTypeDescriptions ]
        }
        else {
            // No high dimensional data found for this term
            [ dataTypes: ["No high dimensional data found"] ]
        }
    }

    /**
     * Converts information about clinical data and high dimensional data into a map
     * that can be handled by the frontend javascript
     * @param clinicalData
     * @param highDimensionalData
     * @see dataTab.js
     * @see ExportService.getClinicalMetaData()
     * @see ExportService.getHighDimMetaData()
     * @return  Map with root key "exportMetaData", which in turn contains a list of
     *              datatypes to export. Each item in the list is a map that has keys,
     *              as below:
     *                  subsetId1
     *                  subsetId2
     *                  subsetName1
     *                  subsetName2
     *
     *                  dataTypeId
     *                  dataTypeName
     *                  isHighDimensional
     *                  metadataExists
     *
     *                  subset1
     *                  subset2
     */
    protected Map convertIntoMetaDataMap( clinicalData, highDimensionalData ) {
        def clinicalOutput = [
                subsetId1: "subset1",
                subsetId2: "subset2",
                subsetName1: "Subset 1",
                subsetName2: "Subset 2",

                dataTypeId: "CLINICAL",
                dataTypeName: "Clinical & Low Dimensional Biomarker Data",
                isHighDimensional: false,
                metadataExists: true,

                subset1: [
                        [
                                fileType: ".TXT",
                                dataFormat: "Data",
                                fileDataCount: clinicalData.subset1,
                                displayAttributes: [:]
                        ]
                ],
                subset2:[
                        [
                                fileType: ".TXT",
                                dataFormat: "Data",
                                fileDataCount: clinicalData.subset2,
                                displayAttributes: [:]
                        ]
                ],
        ]

        // Return a map, suited for the frontend to handle
        [
                exportMetaData: [ clinicalOutput ] + convertHighDimMetaData( highDimensionalData )
        ]
    }

    /**
     * Converts information about high dimensional data into a map
     * that can be handled by the frontend javascript
     * @param highDimensionalData   A list with datatypes that can be exported
     * @return  A list of datatypes to export. Each item in the list is a map that has keys,
     *              as below:
     *                  subsetId1
     *                  subsetId2
     *                  subsetName1
     *                  subsetName2
     *
     *                  dataTypeId
     *                  dataTypeName
     *                  isHighDimensional
     *                  metadataExists
     *
     *                  subset1
     *                  subset2
     * @see dataTab.js
     * @see ExportService.getHighDimMetaData()
     */
    protected def convertHighDimMetaData( highDimensionalData ) {
        // TODO: Support multiple export formats per datatype (e.g. raw data and processed data)
        // See ExportService.getMetaData @ 2e2d53d0cba6f6573bf7636de372b96f25312276 for information
        // on how it was specified previously, as well as on the types of data that were allowed
        // for different datatypes
        highDimensionalData.collect { highDimRow ->
            // Determine the types of files that can be exported for this
            // datatype
            Set<HighDimExporter> exporters = highDimExporterRegistry.getExportersForDataType(
                    highDimRow.datatype.dataTypeName );

            // Determine the data platforms that are present for a given subset
            def platforms = [
                    "subset1": getPlatformsForSubjectSampleMappingList( highDimRow.subset1 ),
                    "subset2": getPlatformsForSubjectSampleMappingList( highDimRow.subset2 )
            ]

            [
                    subsetId1: "subset1",
                    subsetId2: "subset2",
                    subsetName1: "Subset 1",
                    subsetName2: "Subset 2",

                    dataTypeId: highDimRow.datatype.dataTypeName,
                    dataTypeName: highDimRow.datatype.dataTypeDescription,
                    isHighDimensional: true,
                    metadataExists: true,
                    supportedDataConstraints: highDimRow.datatype.supportedDataConstraints,

                    subset1: exporters.collect {
                        [
                                fileType: "." + it.format,
                                dataTypeHasCounts: true,
                                dataFormat: it.description,
                                fileDataCount: highDimRow.subset1 ? highDimRow.subset1.size() : 0,
                                platforms: platforms.subset1,
                                displayAttributes: it.displayAttributes
                        ]
                    },
                    subset2: exporters.collect {
                        [
                                fileType: "." + it.format,
                                dataTypeHasCounts: true,
                                dataFormat: it.description,
                                fileDataCount: highDimRow.subset2 ? highDimRow.subset2.size() : 0,
                                platforms: platforms.subset2,
                                displayAttributes: it.displayAttributes
                        ]
                    }
            ]
        }
    }

    /**
     * Returns a list of unique platforms for a given set of subject sample mappings
     * @param assayList
     * @return  A list of unique platforms, each being a map with the keys
     *              gplId
     *              gplTitle
     *              fileDataCount
     */
    private getPlatformsForSubjectSampleMappingList( Collection<Assay> assayList ) {
        if( !assayList )
            return []

        return assayList*.platform.unique().collect { platform ->
            [
                    gplId: platform.id,
                    gplTitle: platform.title,
                    fileDataCount: assayList.findAll { assay -> assay.platform.id == platform.id }.size()
            ]
        }
    }

}
