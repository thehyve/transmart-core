
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<meta name="layout" content="admin" />
		<title>Create User</title>
	</head>

	<body>
		<div class="body">
			<h1>Create User</h1>
			<g:if test="${flash.message}">
			<div class="message">${flash.message}</div>
			</g:if>
			<g:hasErrors bean="${person}">
			<div class="errors">
				<g:renderErrors bean="${person}" as="list" />
			</div>
			</g:hasErrors>
			<g:form action="save">
				<div class="dialog">
					<table>
					<tbody>
						<tr class="prop">
							<td valign="top" class="name"><label for="username">Username of User:</label></td>
							<td valign="top" class="value ${hasErrors(bean:person,field:'username','errors')}">
								<input type="text" id="username" name="username" value="${person.username?.encodeAsHTML()}"/>
							</td>
						</tr>

                        <tr class="prop">
                            <td valign="top" class="name"><label for="galaxyKey">Galaxy Key:</label></td>
                            <td valign="top" class="value ${hasErrors(bean:person,field:'galaxyKey','errors')}">
                                <input type="text" id="galaxyKey" name="galaxyKey" value="${person.galaxyKey?.encodeAsHTML()}"/>
                            </td>
                        </tr>

						<tr class="prop">
							<td valign="top" class="name"><label for="mailAddress">Email:</label></td>
							<td valign="top" class="value ${hasErrors(bean:person,field:'mailAddress','errors')}">
								<input type="text" id="mailAddress" name="mailAddress" value="${person.mailAddress?.encodeAsHTML()}"/>
							</td>
						</tr>
					</tbody>
					</table>
				</div>

				<div class="buttons">
					<span class="button"><input class="save" type="submit" value="Create" /></span>
				</div>

			</g:form>

		</div>
	</body>
</html>
