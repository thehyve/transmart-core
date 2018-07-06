package org.transmartproject.rest.matchers

import org.grails.web.json.JSONObject
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher

class LinkMatcher extends DiagnosingMatcher<JSONObject> {

    String expectedUrl
    String expectedTitle

    static LinkMatcher hasLink(String url, String title) {
        new LinkMatcher(expectedUrl: url, expectedTitle: title)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {

        JSONObject obj = item
        String url = obj.get('href')

        if (expectedUrl != url) {
            mismatchDescription.appendText("link href did not match:")
            mismatchDescription.appendText(" expecting ").appendValue(expectedUrl)
            mismatchDescription.appendText(" was ").appendValue(url)
            return false
        }

        if (expectedTitle) {
            String title = obj.get('title')
            if (expectedTitle != title) {
                mismatchDescription.appendText("link title did not match:")
                mismatchDescription.appendText(" expecting ").appendValue(expectedTitle)
                mismatchDescription.appendText(" was ").appendValue(title)
                return false
            }
        }

        return true
    }

    @Override
    void describeTo(Description description) {
        description.appendText('link with href ').appendValue(expectedUrl)
        if (expectedTitle) {
            description.appendText(' and with title ').appendValue(expectedTitle)
        }
    }

}