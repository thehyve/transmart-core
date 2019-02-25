package org.transmartproject.core.userquery

import com.fasterxml.jackson.annotation.JsonFormat
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.Constraint

import javax.validation.Valid
import javax.validation.constraints.Size

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING

@Canonical
@CompileStatic
@Deprecated
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

    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Date createDate

    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Date updateDate

}
