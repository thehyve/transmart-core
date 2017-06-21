package smartr

import smartr.session.SessionService

class SmartrController {

    static scope = 'smartRSession'

    SessionService sessionService

    def index() {
        [scriptList: sessionService.availableWorkflows()]
    }

    def abc() {
        render "Hello World"
    }
}
