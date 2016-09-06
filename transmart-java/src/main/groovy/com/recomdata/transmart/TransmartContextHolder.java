package com.recomdata.transmart;

import groovy.lang.Singleton;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Created by Florian Guitton on 22/07/2014.
 */

@Singleton
public class TransmartContextHolder implements ApplicationContextAware {

    private static ApplicationContext appCtx;

    public TransmartContextHolder() {
    }

    /** Spring supplied interface method for injecting app context. */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        appCtx = applicationContext;
    }

    /** Access to spring wired beans. */
    public static ApplicationContext getContext() {
        return appCtx;
    }

    static Object getBean(String name) {
        return getContext().getBean(name);
    }

    public static GrailsApplication getGrailsApplication() {
        return (GrailsApplication) getBean("grailsApplication");
    }

}
