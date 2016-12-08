
	<form>
		<br />
		<br />
		
		<table>
			<tr>
				<td>
					${raw(countData)}
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;
				</td>
			</tr>				
			<tr>
				<td>
					${raw(statisticsData)}
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;
				</td>
			</tr>			
			<tr>
				<td>
					<g:if test="${zipLink}">
						<a class='AnalysisLink' class='downloadLink' href="${zipLink}">Download raw R data</a>
					</g:if>
				</td>
			</tr>			
		</table>
	</form>
