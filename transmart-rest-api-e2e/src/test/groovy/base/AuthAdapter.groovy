package base

import groovyx.net.http.HttpConfig.Request

interface AuthAdapter {
    void authenticate(Request request, String userID)
}