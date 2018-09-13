package org.transmartproject.rest.utils

import org.hamcrest.Matcher

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.mapWith

/**
 * Utility methods to construct matchers to check HAL output
 */
class HalMatcherUtils {

    /**
     * Generic matcher for a hal index response, expecting 2 entries:
     * - selfLink
     * - _embedded[embeddedMatcherMap*key:value]
     * @param selfLink
     * @param embeddedMatcherMap map of key to matcher elements to be expected inside '_embedded'
     * @return
     */
    static Matcher halIndexResponse(String selfLink, Map<String, Matcher> embeddedMatcherMap) {

        allOf(
                hasSelfLink(selfLink),
                hasEntry(
                        is('_embedded'),
                        allOf(
                                embeddedMatcherMap.collect {
                                    hasEntry(is(it.key), it.value)
                                }
                        )
                ),
        )
    }

    /**
     * Matcher for a map entry containing _links.self[href:selfLink]
     * @param selfLink value of the expected link
     * @return matcher
     */
    static hasSelfLink(String selfLink) {
        hasEntry(
                is('_links'),
                hasEntry(
                        is('self'), hasEntry('href', selfLink)
                ),
        )
    }

    static Matcher hasLinks(Map<String, String> linkMap) {
        Map expectedLinksMap = linkMap.collectEntries {
            String tempUrl = "${it.value}"
            [(it.key): ([href: tempUrl])]
        }

        hasEntry(
                is('_links'),
                mapWith(expectedLinksMap),
        )
    }
}
