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
import grails.rest.render.AbstractIncludeExcludeRenderer
import grails.rest.render.AbstractRenderer
import grails.rest.render.ContainerRenderer
import grails.rest.render.RenderContext
import grails.rest.render.RendererRegistry
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.validation.Errors
import org.transmartproject.core.exceptions.InvalidArgumentsException

class HalOrJsonRenderer<T> extends AbstractIncludeExcludeRenderer<T> implements InitializingBean {

    String encoding = GrailsWebUtil.DEFAULT_ENCODING

    HalOrJsonRenderer(Class<T> clazz) {
        super(clazz, [MimeType.HAL_JSON, MimeType.JSON] as MimeType[])
    }

    @Autowired
    RendererRegistry rendererRegistry

    static class HalOrJsonCollectionRenderer<T> extends AbstractRenderer<Collection> implements ContainerRenderer<Collection, T> {

        final Class<T> componentType

        final Class<Collection> targetType = Collection

        final AbstractIncludeExcludeRenderer<T> renderer

        HalOrJsonCollectionRenderer(AbstractIncludeExcludeRenderer<T> renderer) {
            super(Collection, renderer.mimeTypes)
            componentType = renderer.getTargetType()
            this.renderer = renderer
        }

        @Override
        void render(Collection object, RenderContext context) {
            def mimeType = context.acceptMimeType ?: mimeTypes[0]
            def newObject = object

            if (mimeType.name == MimeType.HAL_JSON.name) {
                newObject = new CollectionResponseWrapper(
                        componentType: componentType,
                        collection: object)
            }

            renderer.render newObject, context
        }

    }

    @Override
    void afterPropertiesSet() throws Exception {
        rendererRegistry.addRenderer this
        rendererRegistry.addRenderer new HalOrJsonCollectionRenderer(this)
    }

    @Override
    void render(T object, RenderContext context) {
        def mimeType = getAcceptMimeType(context)

        def contentType = GrailsWebUtil.getContentType mimeType.name, encoding
        context.contentType = contentType

        if (!(object instanceof Errors)) {
            renderJson(object, context, contentType)
        } else {
            throw new InvalidArgumentsException("Errors: $object")
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
