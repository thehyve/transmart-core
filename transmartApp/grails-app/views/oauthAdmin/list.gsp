<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="admin"/>
    <title>Manage client applications</title>
</head>

<body>
<div class="body">
    <h1>Connected client applications</h1>

    <p>
        Manage and configure application's client secrets and IDs.
    </p>

    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>


    <div class="adm-list-toolbar">
        <a href="create">Create application client</a>
    </div>

    <table class="list">
        <thead>
        <tr>
            <g:sortableColumn property="id" title="#" />
            <g:sortableColumn property="clientId" title="Client ID" />
            <g:sortableColumn property="clientSecret" title="Client secret" />
            <g:sortableColumn property="redirectUris" title="Redirect URIs" />
            <g:sortableColumn property="authorizedGrantTypes" title="Authorized grant types " />
            <th>&nbsp;</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${clients}" status="i" var="client">
            <tr class="${(client.clientId in configClientIds) ? 'configclient' : ''}">
                <td>${client.id}</td>
                <td><code>${client.clientId}</code></td>
                <td><em class="remark">(hashed)</em></td>
                <td>
                    <g:each in="${client.redirectUris}" status="j" var="uri">
                        <code>${uri}</code><g:if test="${j!=client.redirectUris.size()}"><br /></g:if>
                    </g:each>
                </td>
                <td>
                    <g:each in="${client.authorizedGrantTypes}" status="j" var="grantType">
                        <code>${grantType}</code><g:if test="${j!=client.authorizedGrantTypes.size()}"><br /></g:if>
                    </g:each>
                </td>
                <td>
                    <g:link action="view" id="${client.id}">View</g:link>
                    <g:if test="${!(client.clientId in configClientIds)}">
                        <g:link action="edit" id="${client.id}">Edit</g:link>
                        <g:link action="delete" id="${client.id}">Delete</g:link>
                    </g:if>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>

    <h3>Notes:</h3>
    <p>
        Clients in <span class="configclient">colour</span> are configured in <code>Config.groovy</code>. To configure it, you have to edit it directly
    in the file.
    </p>
</div>
</body>
</html>