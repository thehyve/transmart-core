package bio

class BioMesh {

    String id
    String name

    static mapping = {
        table 'MESH'
        version false
        cache usage:'read-only'
        id column:'UI', generator:'assigned'
        name column:'MH'
    }
    static constraints = {

    }
}
