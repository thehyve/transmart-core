package annotations;

import org.spockframework.runtime.extension.ExtensionAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skip annotated tests,
 * if the 'v1' API is not supported
 * and the value of the annotation is set to true
 * or
 * the 'v1' API is supported
 * and the value of the annotation is set to false
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtensionAnnotation(RequiresV1ApiSupportExtension.class)
public @interface RequiresV1ApiSupport {
    boolean value();
}
