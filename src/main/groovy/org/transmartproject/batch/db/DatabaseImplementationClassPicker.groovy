package org.transmartproject.batch.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.Oracle
import org.transmartproject.batch.beans.Postgresql

import javax.annotation.PostConstruct
import java.lang.annotation.Annotation

/**
 * Picks a database specific class by reading annotations.
 */
@Component
class DatabaseImplementationClassPicker {
    private static final Map<String, Class<? extends Annotation>> KNOWN_ANNOTATIONS = [
            'org.postgresql.Driver'          : Postgresql,
            'oracle.jdbc.driver.OracleDriver': Oracle,
    ]

    @Autowired
    private Environment env

    private Class<? extends Annotation> annotationToLookFor

    @PostConstruct
    void chooseAnnotation() {
        def driverClassName = env.getProperty('batch.jdbc.driver')
        if (!driverClassName) {
            throw new IllegalArgumentException(
                    'Could not find value for property transmart.jdbc.driver')
        }

        if (!KNOWN_ANNOTATIONS[driverClassName]) {
            throw new IllegalArgumentException(
                    "Driver class $driverClassName not supported")
        }

        annotationToLookFor = KNOWN_ANNOTATIONS[driverClassName]
    }

    Class pickClass(Class... candidates) {
        def ret = candidates.findAll {
            annotationToLookFor in it.annotations*.annotationType()
        }
        if (ret.size() == 1) {
            // we're done
            return ret[0]
        } else if (ret.size() > 1) {
            throw new IllegalArgumentException(
                    "Found more than one class annotated with " +
                            "$annotationToLookFor: $ret")
        }

        // fallback
        ret = candidates.findAll {
            KNOWN_ANNOTATIONS.values().intersect(
                    it.annotations*.annotationType() as List).empty
        }
        if (ret.size() > 1) {
            throw new IllegalArgumentException(
                    "Found more than one class without any database " +
                            "annotation: $ret")
        } else if (ret.empty) {
            throw new IllegalArgumentException("Found no candidate annotated " +
                    "with $annotationToLookFor or no database annotations " +
                    "among the classes $candidates")
        }

        ret[0]
    }

    @SuppressWarnings('UnnecessaryPublicModifier') // actually needed
    public <T> T instantiateCorrectClass(Class<? extends T>... candidates) {
        pickClass(candidates).newInstance()
    }
}
