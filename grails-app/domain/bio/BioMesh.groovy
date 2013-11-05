package bio

class BioMesh {

    String id
    String name
    String code

    static mapping = {
        table 'MESH'
        version false
        cache usage:'read-only'
        id column:'UI', generator:'assigned'
        name column:'MH'
        code column:'MN'
    }
    static constraints = {

    }
}
