package org.transmartproject.db

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.export.Tasks.DataExportFetchTask
import org.transmartproject.export.Tasks.DataExportFetchTaskFactory
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.ontology.ConceptsResource

import static org.transmartproject.core.ontology.OntologyTerm.VisualAttributes.HIGH_DIMENSIONAL

@Transactional
class RestExportService {

    @Autowired
    DataExportFetchTaskFactory dataExportFetchTaskFactory

    @Autowired
    HighDimensionResource highDimensionResourceService

    ConceptsResource conceptsResourceService

    List<File> export(arguments) {
        DataExportFetchTask task = dataExportFetchTaskFactory.createTask(arguments)
        task.getTsv()
    }

    def formatDataTypes(List datatypes){
        //Maybe make this a marshaller.
        def returnDataTypeList = []
        def dataTypesList = []
        datatypes.each { dataTypeMap ->
            dataTypeMap.each {key, value->
                if (key in dataTypesList){
                    returnDataTypeList.each { map ->
                        if (key in map.values()){
                            value.each{it.remove('datatypeCode')}
                            map.get('cohorts').add([concepts:value])
                        }
                    }
                } else{
                    dataTypesList.add(key)
                    def datatypeCode = value.collect({it ->
                        it.get('datatypeCode')})
                    value.each{it.remove('datatypeCode')}
                    def datatypeMap = [dataType:key,
                                       dataTypeCode: datatypeCode[0],
                                       cohorts:[[concepts:value]]]
                    returnDataTypeList.add(datatypeMap)
                }
            }
        }
        returnDataTypeList
    }


    def getDataTypes(List conceptKeysList){
        List cohortDataTypes = []
        Map datatypesMap = [:]
        conceptKeysList.each { conceptKey ->
            OntologyTerm concept = conceptsResourceService.getByKey(conceptKey)
            datatypesMap = getHighDimDataType(concept, datatypesMap)
        }
        cohortDataTypes += datatypesMap
        cohortDataTypes
    }

    def getHighDimDataType(OntologyTerm term, Map datatypesMap) {
        // Retrieve all descendant terms that have the HIGH_DIMENSIONAL attribute
        def terms = term.getAllDescendants() + term
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
            //datatypes contains the number of patients for each datatype.
            def datatypes = highDimensionResourceService.getSubResourcesAssayMultiMap([constraint])
            datatypes.collect({ key, value ->
                String datatype = key.dataTypeDescription
                String datatypeCode = key.dataTypeName
                if (datatype in datatypesMap.keySet()){
                    // term.getPatientCount() can also be value.size(). They give different numbers but I'm not sure
                    // what's the difference
                    datatypesMap[datatype].add([numOfPatients: term.getPatientCount(), conceptPath: term.fullName , datatypeCode: datatypeCode])
                } else{
                    datatypesMap[datatype] = [[numOfPatients: term.getPatientCount(), conceptPath: term.fullName, datatypeCode: datatypeCode]]
                }
            })
            datatypesMap
        }
        else {
            // No high dimensional data found for this term, this means it is clinical data
            String datatype = "Clinical data"
            String datatypeCode = "clinical"
            if (datatype in datatypesMap.keySet()){
                // term.getPatientCount() can also be value.size(). They give different numbers but I'm not sure
                // what's the difference
                datatypesMap[datatype].add([numOfPatients: term.patientCount, conceptPath: term.fullName , datatypeCode: datatypeCode])
            } else{
                datatypesMap[datatype] = [[numOfPatients: term.patientCount, conceptPath: term.fullName, datatypeCode: datatypeCode]]
            }
            datatypesMap
        }
    }

}
