package org.transmart.oauth.command

import grails.validation.Validateable

class ChangePasswordCommand implements Validateable {

    def grailsApplication
    def springSecurityService

    String oldPassword
    String newPassword
    String newPasswordRepeated

    static constraints = {

        oldPassword(blank: false, validator: { oldPsw, thisCmd ->
            if (!thisCmd.springSecurityService.passwordEncoder
                    .isPasswordValid(thisCmd.springSecurityService.currentUser.getPersistentValue('passwd'), oldPsw, null)) {
                'doesNotMatch'
            }
        })

        newPassword(blank: false,
                validator: { newPsw, thisCmd ->
            if (newPsw == thisCmd.oldPassword) {
                'hasToBeChanged'
            } else if (thisCmd.grailsApplication.config.user.password.strength.regex.with { it && !(newPsw ==~ it)}) {
                'lowPasswordStrength'
            }
        })

        newPasswordRepeated(blank: false, validator: { newPsw2, thisCmd ->
            if (newPsw2 != thisCmd.newPassword) {
                'doesNotEqual'
            }
        })

    }

}