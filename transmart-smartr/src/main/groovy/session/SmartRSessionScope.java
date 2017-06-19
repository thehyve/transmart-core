package heim.session;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Needs to be written in Java, otherwise you get
 * Annotation @org.springframework.context.annotation.Scope is not allowed on
 * element ANNOTATION
 */
@Scope(value = "smartRSession", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SmartRSessionScope {
}
