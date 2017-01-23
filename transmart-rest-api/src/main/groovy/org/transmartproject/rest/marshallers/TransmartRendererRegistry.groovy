package org.transmartproject.rest.marshallers

import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.transmartproject.rest.misc.ComponentIndicatingContainer

/**
 * Customized the {@link DefaultRendererRegistry} by making it aware of the
 * {@link ComponentIndicatingContainer}.
 */
class TransmartRendererRegistry extends DefaultRendererRegistry {

    @Override
    protected Class<? extends Object> getTargetClassForContainer(
            Class containerClass, Object object) {
        if (object instanceof ComponentIndicatingContainer) {
            return object.componentType
        }

        super.getTargetClassForContainer(containerClass, object)
    }
}
