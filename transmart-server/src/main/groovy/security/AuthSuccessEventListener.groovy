package security

import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.transmart.oauth2.BruteForceLoginLockService

class AuthSuccessEventListener implements ApplicationListener<AuthenticationSuccessEvent> {

    BruteForceLoginLockService bruteForceLoginLockService

    @Override
    void onApplicationEvent(AuthenticationSuccessEvent event) {
        bruteForceLoginLockService.loginSuccess(event.authentication.name)
    }
}
