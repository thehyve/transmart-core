package org.transmartproject.rest.matchers

import org.grails.web.json.JSONObject
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher

class MetadataTagsMatcher extends DiagnosingMatcher<JSONObject> {
    Map<String, String> expectedTags

    static MetadataTagsMatcher hasTags(Map<String, String> tags) {
        new MetadataTagsMatcher(expectedTags: tags)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {

        JSONObject obj = item
        def hasMetadata = obj.has('metadata')

        if (expectedTags.isEmpty()) {
            if (hasMetadata) {
                mismatchDescription.appendText(" was not expecting any metadata tags, but got them")
                return false
            } else {
                return true
            }
        }

        JSONObject md = obj.getJSONObject('metadata')

        Map map = md as Map
        if (map != expectedTags) {
            mismatchDescription.appendText(" tags did not match")
            mismatchDescription.appendText(" expecting ").appendValue(expectedTags)
            mismatchDescription.appendText(" was ").appendValue(map)
            return false
        }

        return true
    }

    @Override
    void describeTo(Description description) {
        description.appendText(' concept with metadata tags ').appendValue(expectedTags)
    }
}