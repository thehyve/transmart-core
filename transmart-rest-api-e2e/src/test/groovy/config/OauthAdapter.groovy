package config

import base.AuthAdapter
import groovyx.net.http.HttpConfig

import static groovyx.net.http.HttpBuilder.configure

class OauthAdapter implements AuthAdapter {

    private static HashMap<String, User> users = [:]

    OauthAdapter() {
        users.put('test-public-user-1', new User('test-public-user-1', 'test-public-user-1'))
        users.put('test-public-user-2', new User('test-public-user-2', 'test-public-user-2'))
        users.put('ADMIN', new User('admin', 'admin'))
    }

    @Override
    void authenticate(HttpConfig.Request request, String userID) {
        request.headers.'Authorization' = 'Bearer ' + getToken(userID)
    }

    static String getToken(String userID) {
        def user = getUser(userID)
        if (!user.token) {
            user.token = requestToken(user)
        }
        user.token
    }

    static String requestToken(User user) {
        configure {
            request.uri = Config.BASE_URL
        }.post() {
            request.uri.path = '/oauth/token'
            request.uri.query = ['grant_type': 'password', 'client_id': 'glowingbear-js', 'client_secret': '', 'username': user.username, 'password': user.password]
        }.access_token
    }

    static User getUser(String userID) {
        if (!users.get(userID)) {
            throw new MissingResourceException("the user with id ${userID} is not definded in OauthAdapter.users", 'User', userID)
        }
        users.get(userID)
    }

    class User {
        String username
        String password
        String token

        User(String username, String password) {
            this.username = username
            this.password = password
            this.token = null
        }
    }
}