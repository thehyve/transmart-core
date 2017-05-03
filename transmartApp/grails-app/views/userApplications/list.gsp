<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="utilities"/>
    <title>Manage client applications</title>
</head>

<body>
<div class="body">
    <h1>User's connected application settings</h1>


    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>

    <g:if test="${tokens.isEmpty()}">
        <p>None connected to this tranSMART instance using your account credentials.</p>
    </g:if>
    <g:else>

        <p>
            You have granted the following applications access
            to your account.
        </p>

        <div class="adm-list-toolbar">
            <g:link before="return confirm('Are you sure you want to revoke all access tokens?');" action="revokeAll">Revoke all</g:link>
        </div>

        <table class="list">
            <thead>
            <tr>
                <th>#</th>
                <g:sortableColumn property="clientId" title="Client ID" />
                <g:sortableColumn property="expiration" title="Access token expiry" />
                <g:sortableColumn property="refreshToken.expiration" title="Refresh token expiry" />
                %{--<g:sortableColumn property="username" title="Username" />--}%
                %{--<g:sortableColumn property="tokenType" title="Type" />--}%
                <th>&nbsp;</th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${tokens}" status="i" var="token">
                <tr>
                    <td>${token.id}</td>
                    <td>${token.clientId}</td>
                    <td class="${(token.expiration < new Date()) ? 'expired' : ''}">
                        <g:formatDate date="${token.expiration}" format="dd-MM-yyyy hh:mm" />
                    </td>
                    <%
                        def refreshTokenExpiration = refreshTokenExpiration[token.id]
                    %>
                    <td class="${(refreshTokenExpiration && refreshTokenExpiration < new Date()) ? 'expired' : ''}">
                        <g:formatDate date="${refreshTokenExpiration}" format="dd-MM-yyyy hh:mm" />
                    </td>
                    %{--<td>${token.username}</td>--}%
                    %{--<td>${token.tokenType}</td>--}%
                    <td>
                        <g:link before="return confirm('Are you sure you want to revoke the access token?');"
                                      action="revoke" id="${token.id}">Revoke</g:link>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </g:else>


</div>
</body>
</html>