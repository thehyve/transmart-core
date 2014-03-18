package blend4j.plugin

class UserDetails implements Serializable {

    Long id
    String galaxyKey
    String mailAddress

    static mapping = {
        table schema: 'galaxy', name: 'USERS_DETAILS_FOR_EXPORT_TO_GALAXY'
        version false
        id column: 'ID'
        galaxyKey column: 'GALAXY_KEY'
        mailAddress column: 'MAIL_ADDRESS'
    }


    static constraints = {
        id(nullable: false)
    }
}
