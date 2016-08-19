%{--include js lib for ic50 dynamically--}%
<r:require modules="ic50_plot"/>
<r:layoutResources disposition="defer"/>

<div>
	<form>
	
		<table class="subsettable" style="margin: 10px;width:300px; border: 0px none; border-collapse: collapse;" >
			<tr>
				<td colspan="4">
					<span class='AnalysisHeader'>Variable Selection</span>
				</td>			
			</tr>	
			<tr>
				<td colspan="4">
					<hr />
				</td>
			</tr>	
			<tr>
				<td align="center">
					<span class='AnalysisHeader'>Cell Lines</span>
					<br />
					<br />
					Select the categorical variable that represents the cell lines you wish to plot.
				</td>
				<td id="subsetdivider" rowspan="2" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<span class='AnalysisHeader'>Concentration Variable</span>
					<br />
					<br />
					Select the concentration folder from the Data Set Explorer tree and drag it into the box.					
				</td>
			</tr>
			<tr>			
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupIC50('divCellLinesVariable')" value="X"> 
					<br />
					<div id='divCellLinesVariable' class="queryGroupIncludeSmall"></div>
				</td>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupIC50('divConcentrationVariable')" value="X"> 
					<br />
					<div id='divConcentrationVariable' class="queryGroupIncludeSmall"></div>
				</td>			
			</tr>
			
			<tr><td><br/></td></tr>
			<tr>
				<td colspan="4" align="center">
					<input type="button" value="Run" onClick="ic50view.submitIC50Job(this.form);"></input>
				</td>
			</tr>
		</table>
	</form>
</div>
