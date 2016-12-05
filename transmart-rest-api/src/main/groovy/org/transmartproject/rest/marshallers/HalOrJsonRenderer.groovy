/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.marshallers

import grails.converters.JSON
import grails.rest.render.*
import grails.web.mime.MimeType
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import org.transmartproject.core.exceptions.InvalidArgumentsException

import java.util.regex.Pattern

class HalOrJsonRenderer<T> extends AbstractIncludeExcludeRenderer<T> implements InitializingBean {

    private static final Pattern CHARSET_IN_CONTENT_TYPE_REGEXP =
            Pattern.compile(';\\s*charset\\s*=', Pattern.CASE_INSENSITIVE)

    String encoding = 'UTF-8'

    HalOrJsonRenderer(Class<T> clazz) {
        super(clazz, [MimeType.HAL_JSON, MimeType.JSON] as MimeType[])
    }

    @Autowired
    RendererRegistry rendererRegistry

    static class HalOrJsonCollectionRenderer<C, T> extends AbstractRenderer<C>
            implements ContainerRenderer<C, T> {

        final Class<T> componentType

        final Class<C> targetType

        final AbstractIncludeExcludeRenderer<T> renderer

        HalOrJsonCollectionRenderer(AbstractIncludeExcludeRenderer<T> renderer,
                                    Class<C> containerType) {
            super(containerType, renderer.mimeTypes)
            componentType = renderer.getTargetType()
            this.renderer = renderer
            this.targetType = containerType
        }

        @Override
        void render(C object, RenderContext context) {
            def mimeType = context.acceptMimeType ?: mimeTypes[0]
            def newObject = object

            if (mimeType.name == MimeType.HAL_JSON.name) {
                newObject = new ContainerResponseWrapper(
                        componentType: componentType,
                        container: object)
            }

            renderer.render newObject, context
        }

    }

    @Override
    void afterPropertiesSet() throws Exception {
        rendererRegistry.addRenderer this
        rendererRegistry.addRenderer new HalOrJsonCollectionRenderer(this, Collection)
        rendererRegistry.addRenderer new HalOrJsonCollectionRenderer(this, Iterator)
    }

    @Override
    void render(T object, RenderContext context) {
        def mimeType = getAcceptMimeType(context)

        def contentType = getContentType mimeType.name
        context.contentType = contentType

        if (!(object instanceof Errors)) {
            renderJson(object, context, contentType)
        } else {
            throw new InvalidArgumentsException("Errors: $object")
        }
    }

    private String getContentType(String name) {
        if (name.indexOf(';') > -1 &&
                CHARSET_IN_CONTENT_TYPE_REGEXP.matcher(name).find()) {
            name
        } else {
            "$name;charset=${this.encoding}"
        }
    }

    private void renderJson(T object, RenderContext context, String contentType) {
        JSON json        = new JSON()
        json.contentType = contentType

        json.target = object
        json.excludes    = context.excludes
        json.includes    = context.includes

        json.render context.writer
    }

    private MimeType getAcceptMimeType(RenderContext context) {
        context.acceptMimeType ?: mimeTypes[0]
    }

}
