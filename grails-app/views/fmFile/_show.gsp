<!--
  tranSMART - translational medicine data mart
  
  Copyright 2008-2012 Janssen Research & Development, LLC.
  
  This product includes software developed at Janssen Research & Development, LLC.
  
  This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
  as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
  1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
  2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
  
  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
  
 
-->

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

