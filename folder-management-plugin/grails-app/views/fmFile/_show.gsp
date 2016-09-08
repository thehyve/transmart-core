<g:if test="${!layout}">
	<i>No columns have been set up for the file view</i>
</g:if>
<table class="columndetail" style="width: 515px;">
	<tbody>
		<g:each in="${layout}" var="layoutRow">
			<tr class="columnprop">
				<td valign="top" class="columnname">${layoutRow.displayName}</td>
				<td valign="top" class="columnvalue">
					<g:if test="${layoutRow.dataType.toLowerCase().equals('date')}">
						<g:fieldDate bean="${file}" field="${layoutRow.column}" format="yyyy-MM-dd"/>
					</g:if>
                    <g:elseif test="${layoutRow.dataType.toLowerCase().equals('datasize')}">
                        <g:fieldBytes bean="${file}" field="${layoutRow.column}"/>
                    </g:elseif>
                    <g:elseif test="${layoutRow.dataType.toLowerCase().equals('prestring')}">
                        <div style="white-space: pre-wrap">${fieldValue(bean:file,field:layoutRow.column)}</div>
                    </g:elseif>
					
					<g:else> <%-- In all other cases, display as string --%>
						${fieldValue(bean:file,field:layoutRow.column)}
					</g:else>
				</td>
			</tr>
		</g:each>
	</tbody>
</table>

