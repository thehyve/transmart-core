package blend4j.plugin

class GalaxyUserDetails implements Serializable {

    int id
    String username
    String galaxyKey
    String mailAddress

    static mapping = {
        table 'GALAXY.USERS_DETAILS_FOR_EXPORT_GAL'
        version false
        id column: 'ID',
                generator: 'increment',
                params: [sequence: 'GALAXY.HIBERNATE_ID']
        username column: 'USERNAME'
        galaxyKey column: 'GALAXY_KEY'
        mailAddress column: 'MAIL_ADDRESS'
    }


    static constraints = {
        id(nullable: false)
    }
}