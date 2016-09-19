package org.transmartproject.batch.support

import com.google.common.base.MoreObjects
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.AbstractResource
import org.springframework.core.io.PathResource
import org.springframework.core.io.Resource
import org.springframework.util.Assert

import javax.annotation.PostConstruct

/**
 * Resource representing a file whose filename is read from a job parameter.
 * If the file does not exist, returns an empty resource.
 */
@Slf4j
class JobParameterFileResource implements Resource {
    String parameter

    // should be job or step scope
    @Value('#{jobParameters}')
    Map<String, Object> jobParameters

    @Delegate
    Resource resource

    @PostConstruct
    void afterPropertiesSet() {
        Assert.notNull parameter
        if (!jobParameters[parameter]) {
            log.info "Parameter $parameter not specified. " +
                    "Returning empty resource."
            resource = new InexistentResource()
        } else {
            resource = new PathResource(jobParameters[parameter])
        }
    }

    @Override
    String toString() {
        MoreObjects.toStringHelper(this)
                .add("delegateResource", resource)
                .toString()
    }

    class InexistentResource extends AbstractResource {
        final String description = "inexistent resource (parameter $parameter does not exist)"

        final boolean readable = false

        InputStream getInputStream() throws IOException {
            throw new FileNotFoundException(
                    "$description cannot be opened because it does not point to a readable resource")
        }

        boolean exists() {
            false
        }
    }
}
