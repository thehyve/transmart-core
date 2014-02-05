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
<link rel="stylesheet" type="text/css" href="${resource(dir:'css', file:'dataAssociation.css')}">

</head>

<body>
	<form>
	
		<table class="subsettable" style="margin: 10px;width:600px; border: 0px none; border-collapse: collapse;">
			<tr>
				<td colspan="4">
					<span class='AnalysisHeader'>Variable Selection</span>
					 
					<a href='JavaScript:D2H_ShowHelp(1396,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
						<img src="${resource(dir:'images',file:'help/helpicon_white.jpg')}" alt="Help" border=0 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
					</a>
											
				</td>			
			</tr>	
			<tr>
				<td colspan="6">
					<hr />
				</td>
			</tr>	
			<tr>
				<td align="center">
					<span class='AnalysisHeader'>Independent Variable</span>
					<br />
					<br />
					Select <b>one</b> categorical variable from the Data Set Explorer Tree and drag it into the box. (ex. Treatment Arm). A continuous variable may be categorized by enabling "Binning" below. 					
				</td>
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<span class='AnalysisHeader'>Dependent Variable</span>
					<br />
					<br />
					Select <b>two categorical variables</b> variables from the Data Set Explorer Tree and drag them into the box. (ex. Metastasis vs. No Metastasis ). A continuous variable may be categorized by enabling "Binning" below.
				</td>	
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
				<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<span class='AnalysisHeader'>Stratification Variable (Optional)</span>
					<br />
					<br />
					Select <b>one or more categorical variables</b> from the Data Set Explorer Tree and drag them into the box (ex. Race). A continuous variable may be categorized by enabling "Binning" below.
				</td>							
			</tr>
	
			<tr>
				<td align="center">
					<input style="font: 9pt tahoma;float:right;" type="button" onclick="clearGroupForest('divIndependentVariable')" value="X" /> <br />
					<div id='divIndependentVariable' class="queryGroupIncludeSmall  excludeValuePopup leafNodesOnly"></div>
					<!--  <input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divIndependentVariable')" value="High Dimensional Data">-->
					<br />
					
					<span class='AnalysisHeader' style='text-align:center;'>Control or Reference</span>
					<br />
					<br />
					Select <b>one</b> categorical variable from the Data Set Explorer Tree and drag it into the box. (ex. Placebo Arm). A continuous variable may be categorized by enabling "Binning" below. 
					<br />		
					<br />								
					<input style="font: 9pt tahoma;float:right;" type="button" onclick="clearGroupForest('divReferenceVariable')" value="X" /> <br />
					<div id='divReferenceVariable' class="queryGroupIncludeSmall  excludeValuePopup leafNodesOnly"></div>
					<!--  <input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divReferenceVariable')" value="High Dimensional Data">-->					
					
					
				</td>									
				<td align="right" style="vertical-align:top;">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupForest('divDependentVariable')" value="X" /> <br />
					<div id='divDependentVariable' class="queryGroupIncludeLong  excludeValuePopup leafNodesOnly"></div>
					<br />
					<br />
					<input type="checkbox" id="chkAssumeNonEvent" title="Non Event Checkbox" /> Check this box to group together all subjects without a node specified above.					
					<!--  <input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divDependentVariable')" value="High Dimensional Data">-->
				</td>
				<td align="right" style="vertical-align:top;">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupForest('divStratificationVariable')" value="X" /> <br />
					<div id='divStratificationVariable' class="queryGroupIncludeLong  excludeValuePopup leafNodesOnly"></div>
					<!--  <input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divStratificationVariable')" value="High Dimensional Data">-->
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
				<td>
					<div id = "displaydivStratificationVariable"></div>
				</td>
			</tr>
			<tr><td><br/></td></tr>
		</table>

		<table class="subsettable" style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
			<tr>
				<td>
					<span class='AnalysisHeader'>Binning&nbsp;&nbsp;&nbsp;<input id="BinningToggle" type = "button" value="Enable" onClick="toggleBinningForest();" /></span>
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
						<span class='AnalysisHeader'>Reference or Control Variable</span>
					</td>						
					<td id="subsetdivider" rowspan="10" valign="center" align="center" height="100%">
						<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px;"></div>
					</td>					
					<td>
						<span class='AnalysisHeader'>Dependent Variable</span>
					</td>	
					<td id="subsetdivider" rowspan="10" valign="center" align="center" height="100%">
						<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px;"></div>
					</td>
					<td>
						<span class='AnalysisHeader'>Stratification Variable</span>
					</td>					
				</tr>
				<tr>
					<td style="white-space: nowrap;">
						<b>Bin the Independent Variable</b> : <input type="checkbox" id="EnableBinningIndep" title="Independent Binning" />
					</td>		
					<td style="white-space: nowrap;">
						<b>Bin the Reference or Control Variable</b> : <input type="checkbox" id="EnableBinningReference" title="Reference Binning" />
					</td>	
					<td>
						<b>Bin the Dependent Variable</b> : <input type="checkbox" id="EnableBinningDep" title="Dependent Binning" />
					</td>
					<td>
						<b>Bin the Stratification Variable</b> : <input type="checkbox" id="EnableBinningStratification" title="Stratification Binning" />
					</td>
				</tr>
				<tr>
					<td rowspan="3" style="vertical-align:top;">
						<b>Variable Type</b> 
						<select id="variableTypeIndep" onChange="updateManualBinningForest('Indep');">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
						</select>
						<input type="hidden" id="txtNumberOfBinsIndep" value="1" />
						<input type="hidden" id="selBinDistributionIndep" value="" />
						<input type="checkbox" id="chkManualBinIndep"  style="display:none;" onClick="manageBinsForest(document.getElementById('txtNumberOfBinsIndep').value,'Indep');" checked="true" title="Manual Bin Independent" />
						
						<br />
						<br />
						
						<div id="divManualBinContinuousIndep" style="display: none;">
							<table style="border-style: solid; border-width: thin; border-collapse: collapse;"
								id="tblBinContinuousIndep">
								<tr>
									<td colspan="3" align="center">Range</td>
								</tr>
							</table>
						</div>
						
						<div id="divManualBinCategoricalIndep" style="display: none;">
							
							All the items in the Independent variable box will be grouped into one bin.

							<div id='divCategoricalItemsIndep' class="queryGroupIncludeLong" style="display : none;"></div>
							<table id="tblBinCategoricalIndep" style="display : none;"></table>

						</div>							
					</td>			
					<td rowspan="3" style="vertical-align:top;">
						<b>Variable Type</b> 
						<select id="variableTypeReference" onChange="updateManualBinningForest('Reference');">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
						</select>
						<input type="hidden" id="txtNumberOfBinsReference" value="1" />
						<input type="hidden" id="selBinDistributionReference" value="" />
						<input type="checkbox" id="chkManualBinReference"  style="display:none;" onClick="manageBinsForest(document.getElementById('txtNumberOfBinsReference').value,'Reference');" checked="true" title="Manual Bin Reference" />
						
						<br />
						<br />	
						
						<div id="divManualBinContinuousReference" style="display: none;">
							<table style="border-style: solid; border-width: thin; border-collapse: collapse;"
								id="tblBinContinuousReference">
								<tr>
									<td colspan="3" align="center">Range</td>
								</tr>
							</table>
						</div>					
						
						<div id="divManualBinCategoricalReference" style="display: none;">
						
							All the items in the Control or Reference variable box will be grouped into one bin.

							<div id='divCategoricalItemsReference' class="queryGroupIncludeLong" style="display: none;"></div>
							<table id="tblBinCategoricalReference" style="display: none;"></table>

						</div>							
					</td>						
					<td>
						<b>Variable Type</b> 
						<select id="variableTypeDep" onChange="updateManualBinningForest('Dep');">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
						</select>
					</td>	
					<td>
						<b>Variable Type</b> 
						<select id="variableTypeStratification" onChange="updateManualBinningForest('Stratification');">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
						</select>
					</td>									
				</tr>
				<tr>											
					<td align="left">
						<b>Number of Bins</b> : <input type="text" id="txtNumberOfBinsDep" onChange="validateDependentBins(this.value);manageBinsForest(this.value,'Dep');" value="2" title="Number of Bins Dependent" />
					</td>
					<td align="left">
						<b>Number of Bins</b> : <input type="text" id="txtNumberOfBinsStratification" onChange="manageBinsForest(this.value,'Stratification');" value="2" title="Number of Bins Stratification" />
					</td>
				</tr>
				<tr>					
					<td align="left">
						<b>Bin Assignments (Continuous variables only)</b> : 
						<select id="selBinDistributionDep">
								<option value="EDP">Evenly Distribute Population</option>
								<option value="ESB">Evenly Spaced Bins</option>
						</select>
					</td>
					<td align="left">
						<b>Bin Assignments (Continuous variables only)</b> : 
						<select id="selBinDistributionStratification">
								<option value="EDP">Evenly Distribute Population</option>
								<option value="ESB">Evenly Spaced Bins</option>
						</select>
					</td>
				</tr>
				<tr>				
					<td rowspan="2">
						&nbsp;
					</td>	
					<td rowspan="2">
						&nbsp;
					</td>					
				</tr>
				<tr>
					<td>
						<b>Manual Binning</b> : <input type="checkbox" id="chkManualBinDep" onClick="manageBinsForest(document.getElementById('txtNumberOfBinsDep').value,'Dep');" title="Manual Binning Dependent"/>
					</td>	
					<td>
						<b>Manual Binning</b> : <input type="checkbox" id="chkManualBinStratification" onClick="manageBinsForest(document.getElementById('txtNumberOfBinsStratification').value,'Stratification');" title="Manual Bin Stratification" />
					</td>					
				</tr>			
				<tr>
					<td>
						&nbsp;
					</td>	
					<td>
						&nbsp;	
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
					<td>
						<!-- Stratification -->
						<div id="divManualBinContinuousStratification" style="display: none;">
							<table style="border-style: solid; border-width: thin; border-collapse: collapse;" id="tblBinContinuousStratification">
								<tr>
									<td>Bin Name</td>
									<td colspan="2" align="center">Range</td>
								</tr>
							</table>
						</div>
						<div id="divManualBinCategoricalStratification" style="display: none;">
							<table style=" border: 0px none; border-collapse: collapse;">
								<tr>
									<td style="vertical-align: top;"><b>Categories</b>
										<div id='divCategoricalItemsStratification' class="queryGroupIncludeLong"></div>
									</td>
									<td style="vertical-align: top;"><br />
									<br /><-Drag To Bin-></td>
									<td>
										<table id="tblBinCategoricalStratification" style=" border: 0px none; border-collapse: collapse;">

										</table>
									</td>
								</tr>
							</table>
						</div>
					</td>					
				</tr>
			</table>
		</div>
		
		<table class="subsettable" border="0px"	style="margin: 10px;width:600px;">
			<tr>
				<td style="text-align:center;" colspan="5">
					<b>Statistic Type:</b>
					<select id="statistic.type">
							<option value="OR">Odds Ratio</option>
							<option value="RR">Relative Risk</option>
					</select>
					<br />
					<br />
				</td>
			</tr>		
			<tr>
				<td align="center">
					<input type="button" value="Run" onClick="submitTableWithForestJob();" style="float:center;"></input>
				</td>
			</tr>
		</table>		
		
		
	</form>
</body>

</html>