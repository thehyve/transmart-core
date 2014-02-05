package pages.login

import geb.Page

class DeniedPage extends Page {
	static url = "login/denied"

	static at = { title ==~ /(?i)Denied.+/}	
	
}