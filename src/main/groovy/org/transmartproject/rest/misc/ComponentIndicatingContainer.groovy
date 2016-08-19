package org.transmartproject.rest.misc

import grails.rest.render.RendererRegistry
import org.springframework.validation.BeanPropertyBindingResult

/**
 * Grails has two types of renderers: the regular ones and container renderers.
 * For choosing the right container renderer, the {@link RendererRegistry} needs
 * to know the type of objects inside the container.
 *
 * Indicates to the {@link RendererRegistry} the type of elements that this
 * container has inside. This is generally not needed for {@link Iterable},
 * {@link Map}, or {@link BeanPropertyBindingResult objects}, because the
 * registry can already figure out the component type.
 */
interface ComponentIndicatingContainer {
    Class<?> getComponentType()
}
