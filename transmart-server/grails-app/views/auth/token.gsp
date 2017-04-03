<head>
    <meta name='layout' content='main'/>
    <title>${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body>
<div style="text-align: center;">
    <div style="width: 800px; margin: 50px auto 50px auto;">
        <img style="display: block; margin: 12px auto;"
             src="${resource(dir: 'images', file: grailsApplication.config.com.recomdata.largeLogo)}"
             alt="Transmart"/>
        <div style="text-align: center; word-wrap:break-word;"><h3>
            Google access token:</h3>
            <br/>${token}<br/>
        </div>
        <br/><br/>

        <div style="text-align: center; white-space: nowrap">
            <h3>
                Is logged in with Google oauth2 provider:
            </h3>
            <h6>
                <oauth2:ifLoggedInWith provider="google">yes</oauth2:ifLoggedInWith>
                <oauth2:ifNotLoggedInWith provider="google">no</oauth2:ifNotLoggedInWith>
            </h6>
        </div>
    </div>
</div>
</body>
