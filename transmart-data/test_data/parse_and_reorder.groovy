dependsOn = [
	'deapp.de_subject_microarray_data'  : [
		'i2b2demodata.patient_dimension'
	],
        'searchapp.search_auth_group'       : [
                'searchapp.search_auth_principal'
        ],
        'i2b2demodata.trial_visit_dimension': [
                'i2b2demodata.study'
        ],
        'i2b2demodata.observation_fact'     : [
                'i2b2demodata.trial_visit_dimension',
        ],
        'searchapp.search_role_auth_user'   : [
                'searchapp.search_auth_user',
                'searchapp.search_role',
        ],
        'searchapp.search_auth_sec_object_access'   : [
                'searchapp.search_auth_principal',
                'searchapp.search_sec_access_level',
                'searchapp.search_secure_object',
        ],
        'i2b2demodata.qt_query_instance' : [
                'i2b2demodata.qt_query_master',
                'i2b2demodata.qt_query_status_type',
        ],
        'i2b2demodata.qt_query_result_instance' : [
                'i2b2demodata.qt_query_instance',
                'i2b2demodata.qt_query_result_type',
        ],
        'i2b2demodata.qt_patient_set_collection' : [
                'i2b2demodata.qt_query_result_instance',
        ],
        'i2b2demodata.linked_file_collection':[
                'i2b2demodata.storage_system',
                'i2b2demodata.study'
        ],
        'i2b2demodata.relation':[
                'i2b2demodata.relation_type',
                'i2b2demodata.patient_dimension'
        ]
]

def parseTableDataMeta(String filePathLine) {
    File tableDataFile = new File(filePathLine)
    File schemaDirectory = tableDataFile.parentFile
    File tablesSetDirectory = schemaDirectory.parentFile
    String table = "${schemaDirectory.name}.${tableDataFile.name - ~/\.[^.]+$/}"

    [
            tablesSetDirectory: tablesSetDirectory,
            tableDataFile     : tableDataFile,
            table             : table,
    ]
}

def orderDependencies(List loadTablesOrder) {
    dependsOn.each { entry ->
        if (loadTablesOrder.contains(entry.key)) {
            int dependeeIndex = loadTablesOrder.indexOf(entry.key)
            int maxIndex = entry.value.collect { loadTablesOrder.indexOf(it) }.max()
            if (maxIndex > dependeeIndex) {
                String dependeeTable = loadTablesOrder.remove(dependeeIndex)
                loadTablesOrder.add(maxIndex, dependeeTable)
            }
        }
    }
}

List dataTableMetaItems = []
System.in.eachLine { String filePathLine ->
    dataTableMetaItems << parseTableDataMeta(filePathLine)
}

List allTables = dataTableMetaItems.collect { it.table }.unique().sort()

orderDependencies(allTables)

def allTablesSets = dataTableMetaItems.groupBy { it.tablesSetDirectory }.sort()

allTablesSets.each { entry ->
    List tablesDataItems = entry.value.sort {
        allTables.indexOf(it.table)
    }
    tablesDataItems.each { tableDataItem ->
        println([tableDataItem.table, tableDataItem.tableDataFile].join('\t'))
    }
}
