package pages.login

import geb.Page

class IndexPage extends Page {

    static url = "/"

    static at = { title ==~ /(?i)Dunno.*/}

    static content = {
    	loggedinuser(required: false) {$("div", id:"user")}
    }
}
