package org.transmartproject.core.userquery

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Constraint

import javax.validation.Valid
import javax.validation.constraints.Size

@Canonical
@CompileStatic
class UserQueryRepresentation {

    Long id

    @Size(min = 1)
    String name

    @Valid
    Constraint patientsQuery

    Object observationsQuery

    String apiVersion

    Boolean bookmarked

    Boolean subscribed

    SubscriptionFrequency subscriptionFreq

    Object queryBlob

    Date createDate

    Date updateDate

}
