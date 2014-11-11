package org.transmartproject.batch.beans;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Scope(value = "step", proxyMode = ScopedProxyMode.INTERFACES)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StepScopeInterfaced {

}
