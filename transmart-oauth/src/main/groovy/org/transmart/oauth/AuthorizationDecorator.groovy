package org.transmart.oauth

interface AuthorizationDecorator<T> {

    void setDelegate(T delegate)

}
