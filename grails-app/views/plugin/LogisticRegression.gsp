%{--include js lib for box plot dynamically--}%
<r:require modules="logistic_regression"/>
<r:layoutResources disposition="defer"/>

	<form>
	
		<table class="subsettable" border="0px" style="margin: 10px;width:300px;">
			<tr>
				<td colspan="4">
					<span class='AnalysisHeader'>Variable Selection</span>
					<a href='JavaScript:D2H_ShowHelp(1350,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
						<img src="${resource(dir:'images',file:'help/helpicon_white.jpg')}" alt="Help" border=0 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
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
					
					<br /><br />
					Drag a <b>numerical</b> concept from the tree into the box below. The concept must come from a data node (Biomarker Data or Clinical Data).  
					
				</td>
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<span class='AnalysisHeader'>Outcome</span>
					
					<br /><br />
					<p>For the binary response, among two possible outcomes (Commonly the generic terms success and failure are used for these
two outcomes). <br />Drag the two related <b>categorical concepts</b> from the tree into the box below (for example, Subjects with Malignant Vs. Subjects with Benign Tumors). A folder may be dragged in to include the two leaf nodes under that folder. <br /><i>NOTE: The top concept will always be designated a value of 1 and the other a value of 0.</i> 
				</p>
				</td>										
			</tr>
	
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupLine('divDependentVariable')" value="X"> <br />
					<div id='divDependentVariable' class="queryGroupIncludeLong"></div>
				</td>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupLine('divGroupByVariable')" value="X"> <br />
					<div id='divGroupByVariable' class="queryGroupIncludeLong"></div>
				</td>
			</tr>
		</table>
		<table class="subsettable" 
			style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
			<tr>
				<td>
					<span class='AnalysisHeader'>Binning</span>&nbsp;&nbsp;&nbsp;<input id="BinningToggle" type="button" value="Enable" onClick="toggleBinning();" />
				</td>
			</tr>
			<tr>
				<td>
					<hr /></td>
			</tr>
		</table>

		<div id="divBinning" style="display: none;">
			<table id="tblBinningTable" class="subsettable" style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
				<tr>
					<td><b>Variable Type</b> <select id="variableType"
						onChange="updateManualBinningLogisticRegression();">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
					</select></td>
				</tr>
				<tr>
					<td align="left"><b>Number of Bins</b> : 2  
					</td>
				</tr>
				<tr>
					<td align="left"><b>Bin Assignments (Continuous variables only)</b> : 
					<select id="selBinDistribution">
							<option value="EDP">Evenly Distribute Population</option>
							<option value="ESB">Evenly Spaced Bins</option>
					</select></td>
				</tr>
				<tr>
					<td align="left">&nbsp;</td>
				</tr>
				<tr>
					<td align="left"><b>Manual Binning</b> : <input
						type="checkbox" id="chkManualBin" onClick="manageBinsLogisticRegression(2);" title="Manual Binning"/>
					</td>
				</tr>
				<tr>
					<td>&nbsp;</td>
				</tr>
				<tr>
					<td>
						<div id="divManualBinContinuous" style="display: none;">
							<table style="border-style: solid; border-width: thin; border-collapse: collapse;"
								id="tblBinContinuous">
								<tr>
									<td>Bin Name</td>
									<td colspan="2" align="center">Range</td>
								</tr>
							</table>
						</div>
						<div id="divManualBinCategorical" style="display: none;">
							<table style=" border: 0px none; border-collapse: collapse;">
								<tr>
									<td style="vertical-align: top;"><b>Categories</b>
										<div id='divCategoricalItems' class="queryGroupIncludeLong"></div>
									</td>
									<td style="vertical-align: top;"><br />
									<br /><-Drag To Bin-></td>
									<td>
										<table id="tblBinCategorical" style=" border: 0px none; border-collapse: collapse;">

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
					<input type="button" value="Run" onClick="submitLogisticRegressionJob();"></input>
				</td>
			</tr>
		</table>
		
		
	</form>

