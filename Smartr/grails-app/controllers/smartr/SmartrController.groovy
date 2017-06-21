package smartr

import session.SessionService

class SmartrController {

    SessionService sessionService

    def index() {
        [scriptList: sessionService.availableWorkflows()]
    }
}
