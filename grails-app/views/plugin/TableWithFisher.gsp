<!--
 Copyright 2008-2012 Janssen Research & Development, LLC.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>subsetPanel.html</title>

<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
<meta http-equiv="description" content="this is my page">
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">

</head>

<body>
	<form>
	
		<table class="subsettable" style="margin: 10px;width:600px; border: 0px none; border-collapse: collapse;">
			<tr>
				<td colspan="4">
					<span class='AnalysisHeader'>Variable Selection</span>
					<a href='JavaScript:D2H_ShowHelp(1272,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
						<img src="${resource(dir:'images', file:'help/helpicon_white.jpg')}" alt="Help" border=0 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
					</a>						
				</td>			
			</tr>	
			<tr>
				<td colspan="4">
					<hr />
				</td>
			</tr>	
			<tr>
				<td align="center">
					<span class='AnalysisHeader'>Independent Variable</span>
					<br />
					<br />
					Select two or more variables from the Data Set Explorer Tree and drag them into the box.  A continuous variable may be selected and categorized by enabling the "Binning" below.
				</td>
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<span class='AnalysisHeader'>Dependent Variable</span>
					<br />
					<br />
					Select two or more variables from the Data Set Explorer Tree and drag them into the box.  A continuous variable may be selected and categorized by enabling the "Binning" below.
				</td>							
			</tr>
	
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupFisher('divIndependentVariable')" value="X" /> <br />
					<div id='divIndependentVariable' class="queryGroupIncludeLong"></div>
				</td>				
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupFisher('divDependentVariable')" value="X" /> <br />
					<div id='divDependentVariable' class="queryGroupIncludeLong"></div>
				</td>
			</tr>
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divIndependentVariable')" value="High Dimensional Data">
				</td>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divDependentVariable')" value="High Dimensional Data">
				</td>
			</tr>
			<tr><td><br/></td></tr>
			<tr>
				<td>
					<div id="displaydivIndependentVariable"></div>
				</td>
				<td>
					<div id = "displaydivDependentVariable"></div>
				</td>
			</tr>
			<tr><td><br/></td></tr>
		</table>

		<table class="subsettable" style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
			<tr>
				<td>
					<span class='AnalysisHeader'>Binning&nbsp;&nbsp;&nbsp;<input id="BinningToggle" type = "button" value="Enable" onClick="toggleBinningFisher();" /></span>
				</td>
			</tr>
			<tr>
				<td>
					<hr />
				</td>
			</tr>
		</table>

		<div id="divBinning" style="display:none;">
		
			<table id="tblBinningTable" class="subsettable" style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
				<tr>		
					<td>
						<span class='AnalysisHeader'>Independent Variable</span>
					</td>						
					<td id="subsetdivider" rowspan="10" valign="center" align="center" height="100%">
						<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px;"></div>
					</td>
					<td>
						<span class='AnalysisHeader'>Dependent Variable</span>
					</td>					
				</tr>
				<tr>
					<td>
						<b>Bin the Independent Variable</b> : <input type="checkbox" id="EnableBinningIndep" />
					</td>				
					<td>
						<b>Bin the Dependent Variable</b> : <input type="checkbox" id="EnableBinningDep" />
					</td>
				</tr>
				<tr>
					<td>
						<b>Variable Type</b> 
						<select id="variableTypeIndep" onChange="updateManualBinningFisher('Indep');">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
						</select>
					</td>				
					<td>
						<b>Variable Type</b> 
						<select id="variableTypeDep" onChange="updateManualBinningFisher('Dep');">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
						</select>
					</td>									
				</tr>
				<tr>	
					<td align="left">
						<b>Number of Bins</b> : <input type="text" id="txtNumberOfBinsIndep" onChange="manageBinsFisher(this.value,'Indep');" value="2" />
					</td>					
					<td align="left">
						<b>Number of Bins</b> : <input type="text" id="txtNumberOfBinsDep" onChange="manageBinsFisher(this.value,'Dep');" value="2" />
					</td>
				</tr>
				<tr>
					<td align="left">
						<b>Bin Assignments (Continuous variables only)</b> : 
						<select id="selBinDistributionIndep">
								<option value="EDP">Evenly Distribute Population</option>
								<option value="ESB">Evenly Spaced Bins</option>
						</select>
					</td>				
					<td align="left">
						<b>Bin Assignments (Continuous variables only)</b> : 
						<select id="selBinDistributionDep">
								<option value="EDP">Evenly Distribute Population</option>
								<option value="ESB">Evenly Spaced Bins</option>
						</select>
					</td>
				</tr>
				<tr>
					<td>
						&nbsp;
					</td>
					<td>
						&nbsp;
					</td>					
				</tr>
				<tr>
					<td>
						<b>Manual Binning</b> : <input type="checkbox" id="chkManualBinIndep" onClick="manageBinsFisher(document.getElementById('txtNumberOfBinsIndep').value,'Indep');" />
					</td>				
					<td>
						<b>Manual Binning</b> : <input type="checkbox" id="chkManualBinDep" onClick="manageBinsFisher(document.getElementById('txtNumberOfBinsDep').value,'Dep');" />
					</td>					
				</tr>			
				<tr>
					<td>
						<!-- INDEPENDENT -->
						<div id="divManualBinContinuousIndep" style="display: none;">
							<table style="border-style: solid; border-width: thin; border-collapse: collapse;"
								id="tblBinContinuousIndep">
								<tr>
									<td>Bin Name</td>
									<td colspan="2" align="center">Range</td>
								</tr>
							</table>
						</div>
						<div id="divManualBinCategoricalIndep" style="display: none;">
							<table style=" border: 0px none; border-collapse: collapse;">
								<tr>
									<td style="vertical-align: top;"><b>Categories</b>
										<div id='divCategoricalItemsIndep' class="queryGroupIncludeLong"></div>
									</td>
									<td style="vertical-align: top;"><br />
									<br /><-Drag To Bin-></td>
									<td>
										<table id="tblBinCategoricalIndep">

										</table>
									</td>
								</tr>
							</table>
						</div>						
					</td>				
					<td>
						<!-- DEPENDENT -->
						<div id="divManualBinContinuousDep" style="display: none;">
							<table style="border-style: solid; border-width: thin; border-collapse: collapse;" id="tblBinContinuousDep">
								<tr>
									<td>Bin Name</td>
									<td colspan="2" align="center">Range</td>
								</tr>
							</table>
						</div>
						<div id="divManualBinCategoricalDep" style="display: none;">
							<table style=" border: 0px none; border-collapse: collapse;">
								<tr>
									<td style="vertical-align: top;"><b>Categories</b>
										<div id='divCategoricalItemsDep' class="queryGroupIncludeLong"></div>
									</td>
									<td style="vertical-align: top;"><br />
									<br /><-Drag To Bin-></td>
									<td>
										<table id="tblBinCategoricalDep" style=" border: 0px none; border-collapse: collapse;">

										</table>
									</td>
								</tr>
							</table>
						</div>
					</td>					
				</tr>
			</table>
		</div>
		
		<table class="subsettable" border="0px"	style="margin: 10px;width:530px;">
			<tr>
				<td align="center">
					<input type="button" value="Run" onClick="submitTableWithFisherJob();">
				</td>
			</tr>
		</table>		
		
		
	</form>
</body>

</html>