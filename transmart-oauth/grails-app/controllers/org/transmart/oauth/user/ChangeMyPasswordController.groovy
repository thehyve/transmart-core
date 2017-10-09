package org.transmart.oauth.user

import org.springframework.web.servlet.support.RequestContextUtils
import org.transmart.oauth.command.ChangePasswordCommand
import org.transmart.searchapp.AuthUser

class ChangeMyPasswordController {

    static Map allowedMethods = [save: 'POST']
    static defaultAction = "show"

    def messageSource
    def springSecurityService

    def show = {}

    def save(ChangePasswordCommand command) {
        if (command.hasErrors()) {
            render(view: 'show', model: [command: command])
        } else {
            AuthUser currentUser = springSecurityService.currentUser
            currentUser.passwd = springSecurityService.encodePassword(command.newPassword)
            currentUser.changePassword = false
            currentUser.save(flush: true)

            if (currentUser.hasErrors()) {
                command.errors.reject('ChangePassword.couldNotSave')
                render(view: 'show', model: [command: command])
            } else {
                flash.message = messageSource.getMessage('ChangePassword.savedSuccessfully',
                        new Objects[0], RequestContextUtils.getLocale(request))
                redirect(action: 'show')
            }
        }
    }

}

