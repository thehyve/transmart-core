package org.transmartproject.db.ontology

class ModifierDimensionCoreDb {

    String path
    String code
    String name
    Long   level
    String studyId
    String nodeType

    // unused
//    String modifierBlob
//    Date updateDate
//    Date downloadDate
//    Date importDate
//    Long uploadId

    static mapping = {
        table   schema: 'i2b2demodata', name: 'modifier_dimension'
        id      name: 'path', generator: 'assigned'
        version false

        path     column: 'modifier_path'
        code     column: 'modifier_cd'
        name     column: 'name_char'
        level    column: 'modifier_level'
        studyId  column: 'sourcesystem_cd'
        nodeType column: 'modifier_node_type' // known values: {L, F}
    }

    static constraints = {
        path           maxSize:  700
        code           nullable: true, maxSize: 50
        name           nullable: true, maxSize: 2000
        level          nullable: true
        studyId        nullable: true, maxSize: 50
        nodeType       nullable: true, maxSize: 10

        // unused:
//        modifierBlob   nullable: true
//        updateDate     nullable: true
//        downloadDate   nullable: true
//        importDate     nullable: true
//        uploadId       nullable: true
    }
}
