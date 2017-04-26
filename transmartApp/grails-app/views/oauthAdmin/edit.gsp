<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="admin"/>
    <title>Edit client applications</title>
</head>

<body>
<div class="body">
    <h1>Edit client application</h1>

    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${client}">
        <div class="errors">
            <g:renderErrors bean="${client}" as="list" />
        </div>
    </g:hasErrors>

    <g:form action="save" class="adm-frm">

        <input type="hidden" name="id" value="${client.id}" />

        <div class="dialog">

            <div class="adm-input-group ${hasErrors(bean:client,field:'clientId','errors')}">
                <label for="clientId">Client ID</label>
                <input type="text" id="clientId" name="clientId" required maxlength="128" value="${client.clientId?.encodeAsHTML()}"/>
            </div>

            <div class="adm-input-group ${hasErrors(bean:client,field:'clientSecret','errors')}">
                <label for="clientSecret">Client secret</label>
                <div style="display: inline-block;">
                    <input type="text" id="clientSecret" name="clientSecret" ${client.clientSecret ? '':'required'} maxlength="512" value=""/>
                    <g:if test="${client.clientSecret}"><br /><em class="remark adm-view-val">Not changed if empty.</em></g:if>
                </div>
            </div>

            <div  class="adm-input-group">
                <label>OAuth grant type </label>
                <div class="checklist">
                    <div>
                        <input type="checkbox" name="authorizedGrantTypes"
                               id="grant-auth-code" value="authorization_code"
                            ${('authorization_code' in client.authorizedGrantTypes) ? 'checked' : ''} />
                        <label for="grant-auth-code">Authorization code</label>
                    </div>
                    <div>
                        <input type="checkbox" name="authorizedGrantTypes"
                               id="grant-implicit-grant" value="implicit"
                            ${('implicit' in client.authorizedGrantTypes) ? 'checked' : ''} />
                        <label for="grant-implicit-grant">Implicit grant</label>
                    </div>
                    <div>
                        <input type="checkbox" name="authorizedGrantTypes"
                               id="grant-password" value="password"
                            ${('password' in client.authorizedGrantTypes) ? 'checked' : ''} />
                        <label for="grant-password">Password</label>
                    </div>
                    <div>
                        <input type="checkbox" name="authorizedGrantTypes"
                               id="grant-refresh-token" value="refresh_token"
                            ${('refresh_token' in client.authorizedGrantTypes) ? 'checked' : ''} />
                        <label for="grant-refresh-token">Refresh token</label>
                    </div>
                </div>
            </div>

            <div class="adm-input-group">
                <label for="mgr-oauth-redirect-uri">Redirect URIs</label>
                <div style="display: inline-block;">
                    <div id="uris">
                        <g:each in="${client.redirectUris}" status="i" var="uri">
                            <input type="text" name="redirectUris[${i}]" id="mgr-oauth-redirect-uri[${i}]" value="${uri?.encodeAsHTML()}"/><br />
                        </g:each>
                    </div>
                    <script type="application/javascript">
                        var uriCount = ${client.redirectUris.size()};
                        function newUriField(uri) {
                            return $("<input type='text' name='redirectUris[" + (uriCount++) + "]' maxlength='2083' value='" + uri + "' /><br />");
                        }
                        var baseUri = location.protocol+'//'+location.hostname+(location.port ? ':'+location.port: '')+'${request.getContextPath()}';
                        if (uriCount == 0) {
                            jQuery('#uris').append(newUriField(baseUri + '/oauth/verify'));
                        }
                        jQuery('#uris').append(newUriField(''));
                    </script>
                    <p style="text-align: right;">
                        <input onclick="javascript:jQuery('#uris').append(newUriField(''));" type="button" value="Add row" />
                    </p>
                </div>
            </div>

            <div class="buttons" style="text-align: right;">
                <span class="button"><g:actionSubmit class="save" value="Save" /></span>
            </div>

        </div>

    </g:form>

</div>
</body>
</html>