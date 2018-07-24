package org.transmartproject.db.userqueries

import grails.transaction.Transactional
import grails.util.Holders
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.concept.ConceptsResource
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.exceptions.ServiceNotAvailableException
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.ConstraintFactory
import org.transmartproject.core.userquery.SubscriptionFrequency
import org.transmartproject.core.userquery.UserQuery
import org.transmartproject.core.userquery.UserQueryRepresentation
import org.transmartproject.core.userquery.UserQueryResource
import org.transmartproject.core.userquery.UserQuerySetResource
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.LegacyAuthorisationChecks
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.User
import org.transmartproject.db.clinical.ConstraintAccessChecker
import org.transmartproject.db.querytool.Query

import java.util.stream.Collectors

@Transactional
@CompileStatic
class UserQueryService implements UserQueryResource {

    @Autowired
    UserQuerySetResource userQuerySetResource

    @Autowired
    AuthorisationChecks authorisationChecks

    @Autowired
    LegacyAuthorisationChecks legacyAuthorisationChecks

    @Autowired
    ConceptsResource conceptsResource


    private static void validateSubscriptionEnabled(Boolean subscribed, SubscriptionFrequency subscriptionFreq) {
        boolean subscriptionFreqSpecified = subscriptionFreq != null
        boolean subscriptionEnabled = Holders.config.getProperty('org.transmartproject.notifications.enabled', Boolean)
        if (!subscriptionEnabled && (subscribed || subscriptionFreqSpecified)) {
            throw new ServiceNotAvailableException(
                    "Subscription functionality is not enabled. Saving subscription data not supported.")
        }
    }

    static UserQueryRepresentation toRepresentation(UserQuery query) {
        query.with {
            new UserQueryRepresentation(
                    id,
                    name,
                    patientsQuery ? ConstraintFactory.read(patientsQuery) : null,
                    observationsQuery ? BindingHelper.objectMapper.readValue(observationsQuery, Object) : null,
                    apiVersion,
                    bookmarked,
                    subscribed,
                    subscriptionFreq,
                    queryBlob ? BindingHelper.objectMapper.readValue(queryBlob, Object) : null,
                    createDate,
                    updateDate,
            )
        }
    }

    void checkConstraintAccess(Constraint constraint, User currentUser) {
        new ConstraintAccessChecker(currentUser, PatientDataAccessLevel.MEASUREMENTS,
                authorisationChecks, legacyAuthorisationChecks, conceptsResource)
                .build(constraint)
    }

    @Override
    List<UserQueryRepresentation> list(User currentUser) {
        def result = Query.createCriteria().list {
            eq 'username', currentUser.username
            eq 'deleted', false
        } as List<UserQuery>
        result.stream()
                .map({UserQuery query -> toRepresentation(query)})
                .collect(Collectors.toList())
    }

    protected Query fetch(Long id, User currentUser) {
        def query = Query.createCriteria().get {
            eq 'id', id
            eq 'username', currentUser.username
            eq 'deleted', false
        } as Query
        if (!query) {
            throw new NoSuchResourceException("Query with id ${id} not found for user.")
        }
        query
    }

    @Override
    UserQueryRepresentation get(Long id, User currentUser) {
        toRepresentation(fetch(id, currentUser))
    }

    @Override
    UserQueryRepresentation create(UserQueryRepresentation representation, User currentUser) {
        def query = new Query(username: currentUser.username)
        if (representation.name ==  null || representation.name.trim().empty) {
            throw new InvalidArgumentsException("Query name is empty.")
        }
        validateSubscriptionEnabled(representation.subscribed, representation.subscriptionFreq)
        if (representation.subscribed) {
            if (!representation.patientsQuery) {
                throw new InvalidArgumentsException("Cannot subscribe to an empty query.")
            }
            // Check query access when subscription is enabled
            checkConstraintAccess(representation.patientsQuery, currentUser)
        }

        query.with {
            name = representation.name
            patientsQuery = representation.patientsQuery.toJson()
            observationsQuery = BindingHelper.objectMapper.writeValueAsString(representation.observationsQuery)
            bookmarked = representation.bookmarked ?: false
            subscribed = representation.subscribed ?: false
            subscriptionFreq = representation.subscriptionFreq
            queryBlob = BindingHelper.objectMapper.writeValueAsString(representation.queryBlob)
            apiVersion = representation.apiVersion
        }

        query = save(query, currentUser)

        def result = toRepresentation(query)

        if (query.subscribed) {
            // Create initial patient set when subscription is enabled
            userQuerySetResource.createSetWithInstances(result, currentUser)
        }

        result
    }

    @Override
    UserQueryRepresentation update(Long id, UserQueryRepresentation representation, User currentUser) {
        validateSubscriptionEnabled(representation.subscribed, representation.subscriptionFreq)

        UserQuery query = fetch(id, currentUser)

        if (representation.name != null) {
            query.name = representation.name
        }
        if (representation.bookmarked != null) {
            query.bookmarked = representation.bookmarked
        }
        boolean newSubscription = false
        if (representation.subscribed != null) {
            if (representation.subscribed) {
                if (!query.patientsQuery) {
                    throw new InvalidArgumentsException("Cannot subscribe to an empty query.")
                }
                // Check query access when subscription is enabled
                checkConstraintAccess(ConstraintFactory.read(query.patientsQuery), currentUser)
                if (!query.subscribed) {
                    // This is a new subscription, an initial patient set needs to be generated
                    newSubscription = true
                }
            }
            query.subscribed = representation.subscribed
        }
        if (representation.subscriptionFreq != null) {
            query.subscriptionFreq = representation.subscriptionFreq
        }

        query = save(query, currentUser)

        def result = toRepresentation(query)

        if (newSubscription) {
            // Create initial patient set when subscription is being enabled
            userQuerySetResource.createSetWithInstances(result, currentUser)
        }

        result
    }

    protected UserQuery save(UserQuery query, User currentUser) {
        assert query instanceof Query
        if (currentUser.username != query.username) {
            throw new AccessDeniedException("Query does not belong to the current user.")
        }
        if (!query.validate()) {
            def message = query.errors.allErrors*.defaultMessage.join('.')
            throw new InvalidArgumentsException(message)
        }
        query.updateUpdateDate()
        query.save(flush: true, failOnError: true)
        query
    }

    @Override
    void delete(Long id, User currentUser) {
        def query = fetch(id, currentUser)
        assert query instanceof Query
        query.deleted = true
        save(query, currentUser)
    }

}
