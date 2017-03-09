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

        <table style="width:100%; height:150px; border:none;" >
            <tr style="background: none !important">
                <td style="width:200px; vertical-align:center; text-align:center;">
                    <br/>
                    <h3>
                        <oauth2:connect style="color: black" provider="google" id="google-connect-link">Get access token</oauth2:connect>
                    </h3>
                    <br/>
                    <img style="margin:2px; max-width:70px; text-align: center"
                         src="${resource(dir: 'images', file: 'google_logo.png')}"
                         alt="Google"/>

                </td>
                <td style="border-left: 1px solid; vertical-align:center; text-align:center">
                    <br/>
                    <h3>
                        <a style="color: black" href="http://example.com/">Login</a>
                    </h3>
                </td>
            </tr>
        </table>
        <br/><br/>
        <div style="text-align: center;"><h3>${grailsApplication.config.com.recomdata.appTitle}</h3>
        </div>

        <div style="text-align: center; margin: 12px;">
            <h3>Current user: ${user}</h3>
        </div>
    </div>
</div>
</body>
