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
