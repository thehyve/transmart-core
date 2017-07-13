package org.transmart.oauth

import com.github.scribejava.apis.google.GoogleToken
import grails.converters.JSON
import grails.transaction.Transactional

@Transactional
class AuthService {


    def saveToken(GoogleToken tokenString) {
        def token = JSON.parse(tokenString)
    }
}
