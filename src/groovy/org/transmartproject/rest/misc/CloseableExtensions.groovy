package org.transmartproject.rest.misc

import groovy.transform.CompileStatic

@CompileStatic
class CloseableExtensions {

    /**
     * We need Groovy 2.3's withCloseable
     * As we don't have that yet this is a simple copy. Remove this after we upgraded to Groovy 2.3+
     *
     * @param resource the Closeable resource that will be closed after use
     * @param c the action before closing the resource
     * @return the value of the closure
     */
    static def with(Closeable resource, Closure c) {
        try {
            return c.call(resource)
        } finally {
            resource.close()
        }
    }

}
