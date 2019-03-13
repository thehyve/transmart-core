package org.transmartproject.core.userquery

import com.fasterxml.jackson.annotation.JsonCreator
import groovy.transform.CompileStatic

/**
 * The frequency of emails for user query subscription
 * @deprecated user queries related functionality has been moved to a gb-backend application
 */
@CompileStatic
@Deprecated
enum SubscriptionFrequency {

    DAILY,
    WEEKLY

    private static final Map<String, SubscriptionFrequency> mapping = new HashMap<>()
    static {
        for (SubscriptionFrequency type: values()) {
            mapping.put(type.name().toLowerCase(), type)
        }
    }

    @JsonCreator
    static SubscriptionFrequency forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            return null
        }
    }

}
