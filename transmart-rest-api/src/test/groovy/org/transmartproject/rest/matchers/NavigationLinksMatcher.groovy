package org.transmartproject.rest.matchers

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.hamcrest.Matcher
import org.hamcrest.Matchers

class NavigationLinksMatcher extends DiagnosingMatcher<JSONObject> {

    Matcher selfLinkMatcher
    Matcher parentLinkMatcher
    Matcher childrenMatcher

    static NavigationLinksMatcher hasNavigationLinks(String selfLink,
                                                     String parentLink,
                                                     String... children) {

        def slm = LinkMatcher.hasLink(selfLink, null)
        def plm = parentLink ? LinkMatcher.hasLink(parentLink, null) : null

        def baseUrl = selfLink.endsWith('ROOT') ? selfLink.substring(0, selfLink.indexOf('/ROOT')) : selfLink
        List<Matcher> childrenMatchers = children.collect {
            LinkMatcher.hasLink("$baseUrl/$it", it)
        }

        def cm = childrenMatchers.isEmpty() ? null : Matchers.containsInAnyOrder(childrenMatchers.collect { it })

        new NavigationLinksMatcher(selfLinkMatcher: slm, parentLinkMatcher: plm, childrenMatcher: cm)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {

        JSONObject obj = item
        if (!obj.has('_links')) {
            mismatchDescription.appendText("no '_links' was found")
            return false
        }
        JSONObject links = obj.getJSONObject('_links')

        JSONObject self = links.getJSONObject('self')
        if (!self) {
            mismatchDescription.appendText("no 'self' was found")
            return false
        }

        boolean result = selfLinkMatcher.matches(self, mismatchDescription)

        if (!result) {
            return false
        }

        if (parentLinkMatcher) {
            JSONObject parent = links.getJSONObject('parent')
            if (!parent) {
                mismatchDescription.appendText("no 'parent' was found")
                result = false
            } else {
                result = parentLinkMatcher.matches(parent, mismatchDescription)
            }
        }

        if (!result) {
            return false
        }

        def hasChildren = links.has('children')

        if (childrenMatcher) {
            if (hasChildren) {
                JSONArray children = links.getJSONArray('children')
                result = childrenMatcher.matches(children)
            } else {
                mismatchDescription.appendText("no 'children' was found")
                result = false
            }
        } else if (hasChildren) {
            mismatchDescription.appendText("not expected 'children' was found")
            result = false
        }

        return result
    }

    @Override
    void describeTo(Description description) {
        description.appendText("'self' with ")
        selfLinkMatcher.describeTo(description)
        if (parentLinkMatcher) {
            description.appendText(" and 'parent' with ")
            parentLinkMatcher.describeTo(description)
        }
        if (childrenMatcher) {
            description.appendText(" and 'children' with ")
            childrenMatcher.describeTo(description)
        }
    }
}