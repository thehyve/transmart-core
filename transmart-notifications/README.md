## Transmart notifications

Plugin providing a notification functionality for TranSmart users. It sends automated notification emails to subscribers with a daily or weekly digest.

Currently it supports a query subscription and notifies users when data linked to a query they subscribed to changes.


### Configuration of the cron job scheduler:

```
# To disable the email sending job, change this to false
quartz:
    autoStartup: true
```

### Configuration an the email server:

```
grails:
    ...
    mail:
        host: localhost:25
        'default':
            from: <default_email>
        port: 465
        username: <username>
        password: <password>
        props:
           "mail.smtp.auth": true
           "mail.smtp.ssl.enable": true
           "mail.smtp.socketFactory.port": 465
           "mail.smtp.socketFactory.class": javax.net.ssl.SSLSocketFactory
           "mail.smtp.socketFactory.fallback": false
```

For more information check [Grails Email Plugin documentation.](http://gpc.github.io/grails-mail/guide/2.%20Configuration.html)

### Other subscription settings:

```
org.transmartproject.notifications:
    # enable daily and weekly notification jobs
    enabled: true
    # max number of query sets returned in a subscription email
    maxNumberOfSets: 20
    # daily cron job trigger time in format: hh-mm
    # hh: Hour, range: 0-23;
    # mm: Minute, range: 0-59;
    dailyJobTriggerTime: 0-0
```

