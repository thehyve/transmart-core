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
        <div style="text-align: center;"><h3>Logged in successfully${grailsApplication.config.com.recomdata.appTitle}</h3></div>

        <div style="text-align: center; margin: 12px;">
            <h3>Current user: ${user}</h3>
        </div>
    </div>
</div>
</body>
