package smartR

import session.SessionService

class SmartRController {

    SessionService sessionService

    static layout = 'smartR'

    def index() {
        [ scriptList: sessionService.availableWorkflows()]
    }
}
