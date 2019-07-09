package tests.rest.v2

import base.RESTSpec
import base.RestHelper
import org.junit.Ignore
import org.transmartproject.core.multidimquery.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*

/**
 * These tests requires transmart-notifications plugin and /v2/admin/notifications/notify endpoint
 * to be enabled and properly configured.
 * See transmart-notifications configuration description.
 * @deprecated user queries related functionality has been moved to a gb-backend application
 */
@Deprecated
@Ignore
class NotificationsSpec extends RESTSpec {

    void 'test triggering email sending by a regular user is denied'() {
        when: "I try to trigger email sending as default user"
        def frequency = "DAILY"
        def request = [
                path      : PATH_NOTIFICATIONS,
                acceptType: JSON,
                query     : [frequency: frequency],
                user      : DEFAULT_USER,
                statusCode: 403
        ]
        def responseData = RestHelper.toObject get(request), ErrorResponse

        then: "I do not have an access"
        responseData.status == 403
        responseData.error == 'Forbidden'
        responseData.message == 'Access is denied'
    }

    void 'test invalid frequency parameter'() {
        when: "I try to trigger email without frequency parameter"
        def request = [
                path      : PATH_NOTIFICATIONS,
                acceptType: JSON,
                user      : ADMIN_USER,
                statusCode: 400
        ]
        def responseData = RestHelper.toObject get(request), ErrorResponse

        then: "I get InvalidArgumentsException"
        responseData.httpStatus == 400
        responseData.message == 'Invalid frequency parameter: null'
        responseData.type == 'InvalidArgumentsException'

        when: "I try to trigger email with invalid frequency parameter"
        def frequency = "YEARLY"
        def request2 = [
                path      : PATH_NOTIFICATIONS,
                acceptType: JSON,
                user      : ADMIN_USER,
                query     : [frequency: frequency],
                statusCode: 400
        ]
        def responseData2 = RestHelper.toObject get(request2), ErrorResponse

        then: "I get InvalidArgumentsException"
        responseData2.httpStatus == 400
        responseData2.message == 'Invalid frequency parameter: YEARLY'
        responseData2.type == 'InvalidArgumentsException'
    }

    void 'test triggering email by a ADMIN'() {
        def frequency = "DAILY"
        expect: "I try to trigger email sending as ADMIN"
        null == get([
                path      : PATH_NOTIFICATIONS,
                acceptType: JSON,
                query     : [frequency: frequency],
                user      : ADMIN_USER,
                statusCode: 200
        ])
    }
}
