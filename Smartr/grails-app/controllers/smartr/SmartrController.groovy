package smartr

import session.SessionService

class SmartrController {

    static scope = 'smartR'

    SessionService sessionService

    def index() {
        [scriptList: sessionService.availableWorkflows()]
    }

    def abc() {
        render "Hello World"
    }
}
