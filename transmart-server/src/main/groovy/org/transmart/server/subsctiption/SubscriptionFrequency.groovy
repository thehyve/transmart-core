package org.transmart.server.subsctiption

import groovy.transform.CompileStatic

/**
 * The frequency of emails for user query subscription
 */
@CompileStatic
enum SubscriptionFrequency {

    DAILY ('DAILY'),
    WEEKLY ('WEEKLY')

    private String subscriptionFrequency

    SubscriptionFrequency(String subsciptionFrequency) {
        this.subscriptionFrequency = subsciptionFrequency
    }

    static SubscriptionFrequency from(String frequency) {
        SubscriptionFrequency f = values().find { it.subscriptionFrequency == frequency }
        if (f == null) throw new Exception("Unknown frequency type: ${frequency}")
        f
    }

    String value() {
        subscriptionFrequency
    }
}
