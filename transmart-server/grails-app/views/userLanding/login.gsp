<head>
    <meta name='layout' content='main'/>
    <title>${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body>
<div style="text-align: center;">
    <div style="width: 400px; margin: 50px auto 50px auto;">
        <img style="display: block; margin: 12px auto;"
             src="${resource(dir: 'images', file: grailsApplication.config.com.recomdata.largeLogo)}"
             alt="Transmart"/>

        <div style="text-align: center;"><h3>${grailsApplication.config.com.recomdata.appTitle}</h3>
        </div>

        <div style="text-align: center; margin: 12px;">
            <h3>Current user: <b>${user}</b></h3>
        </div>
        <br/>

        <div>
            <h3>Click<b>
                <oauth2:connect provider="google" id="google-connect-link">here</oauth2:connect>
            </b>to authenticate with <img style="margin:2px; max-width:70px; text-align: center"
                                          src="${resource(dir: 'images', file: 'google_logo.png')}"
                                          alt="Google"/>.</h3>
            <br/>

            <div style="text-align: center; white-space: nowrap">
                <h3>
                    Is already logged in with Google oauth2 provider:
                </h3>
                <h6><b>
                    <oauth2:ifLoggedInWith provider="google">yes</oauth2:ifLoggedInWith>
                    <oauth2:ifNotLoggedInWith provider="google">no</oauth2:ifNotLoggedInWith>
                </b></h6>
            </div>
        </div>
    </div>
</div>
</body>
