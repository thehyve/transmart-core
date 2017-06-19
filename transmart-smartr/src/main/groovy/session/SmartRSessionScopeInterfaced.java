package heim.session;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Variant of {@link SmartRSessionScope} that creates interface proxies.
 */
@Scope(value = "smartRSession", proxyMode = ScopedProxyMode.INTERFACES)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SmartRSessionScopeInterfaced {
}
