package org.transmartproject.api.server.server

import org.springframework.security.core.AuthenticationException

class TransmartAuthenticationException extends AuthenticationException {
    TransmartAuthenticationException(String msg, Throwable t) {
        super(msg, t)
    }

    TransmartAuthenticationException(String msg) {
        super(msg)
    }
}
