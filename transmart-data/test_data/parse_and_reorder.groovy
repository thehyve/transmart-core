def dependsOn = [
        'searchapp.search_auth_group'       : [
                'searchapp.search_auth_principal'
        ],
        'i2b2demodata.trial_visit_dimension': [
                'i2b2demodata.study'
        ],
        'i2b2demodata.observation_fact'     : [
                'i2b2demodata.trial_visit_dimension',
        ]
]

def composeTableFileMap(String filePathLine) {
    File file = new File(filePathLine)
    String table = "${file.parentFile.name}.${file.name - ~/\.[^.]+$/}"

    [(table): file]
}

def dataTableFileMap = [:]
System.in.eachLine { String filePathLine ->
    dataTableFileMap << composeTableFileMap(filePathLine)

}

List loadTablesOrder = dataTableFileMap.keySet().toList()
dependsOn.each { entry ->
    if (dataTableFileMap.containsKey(entry.key)) {
        int dependeeIndex = loadTablesOrder.indexOf(entry.key)
        int maxIndex = entry.value.collect { loadTablesOrder.indexOf(it) }.max()
        if (maxIndex > dependeeIndex) {
            String dependeeTable = loadTablesOrder.remove(dependeeIndex)
            loadTablesOrder.add(maxIndex, dependeeTable)
        }
    }
}

loadTablesOrder.each { String table ->
    println([table, dataTableFileMap[table]].join('\t'))
}
