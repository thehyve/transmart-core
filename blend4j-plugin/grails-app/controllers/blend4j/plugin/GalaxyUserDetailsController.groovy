package blend4j.plugin

class GalaxyUserDetailsController {

    def galaxyUserDetailsService;

    def list = {
        if (!params.max) {
            params.max = 999999
        }
        [personList: GalaxyUserDetails.list(params)]
    }

//    def show = {
//        def person = UserDetails.get(params.id)
//        if (!person) {
//            flash.message = "Galaxy User not found with id $params.id"
//            redirect action: list
//            return
//        }
//        [person: person]
//    }

//    def edit = {
//        def person = UserDetails.get(params.id)
//        if (!person) {
//            flash.message = "Galaxy User not found with id $params.id"
//            redirect action: list
//            return
//        }
//        return person
//    }

    /**
     * Person update action.
     */
    def update = {
        def person = GalaxyUserDetails.get(params.id)
        person.properties = params

        if (params.mailAddress == null || params.mailAddress == "") {
            flash.message = 'Please enter an email'
            return render(view: 'edit', model: [person: new GalaxyUserDetails(params)])
        }

    }

    def create = {
        [person: new GalaxyUserDetails(params)]
    }

    def delete = {
        flash.message = 'User deleted';
        System.err.println(params);
        galaxyUserDetailsService.deleteUser(params.id);
        render(view: 'list')
    }

    /**
     * Person save action.
     */
    def save = {

        if (params.username == null || params.username == "") {
            flash.message = 'Please enter a username'
            return render(view: 'create', model: [person: new GalaxyUserDetails(params)])
        }
        if (params.galaxyKey == null || params.galaxyKey == "") {
            flash.message = 'Please enter a Galaxy Key'
            return render(view: 'create', model: [person: new GalaxyUserDetails(params)])
        }
        if (params.mailAddress == null || params.mailAddress == "") {
            flash.message = 'Please enter an email'
            return render(view: 'create', model: [person: new GalaxyUserDetails(params)])
        }

        Boolean isSaved = galaxyUserDetailsService.saveNewGalaxyUser(params.username, params.galaxyKey, params.mailAddress)
        if (isSaved) {
            flash.message = 'User Created'

        }else{
            flash.message = 'Cannot create user'
        }
        render(view: 'create', model: [person: new GalaxyUserDetails()])
    }

}
