package pages.login

import geb.Page

class LoginPage extends Page {

    static url = "login/auth"

    static at = { title ==~ /(?i)Login.*/ }

    static content = {
    	loginMessage {$("div.login_message")}
       loginForm { $("form") }
       loginButton { $("input", value: "Login") }
    }
}