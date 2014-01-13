%{--include js lib for box plot dynamically--}%
<r:require modules="box_plot"/>
<r:layoutResources disposition="defer"/>

<form>
	
		<table class="subsettable" style="margin: 10px;width:300px; border: 0px none; border-collapse: collapse;">
			<tr>
				<td colspan="4">
					<span class='AnalysisHeader'>Variable Selection</span>
					<a href='JavaScript:D2H_ShowHelp(1310,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
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
					Select a variable  from the Data Set Explorer Tree and drag it into the box.  At least one of the variables selected should be a continuous variable (ex. Age) and one should be a categorical variable (ex. Tumor Stage).  A continuous variable can be categorized using the binning option below.
				</td>
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<span class='AnalysisHeader'>Dependent Variable</span>
					<br />
					<br />
					Select a variable  from the Data Set Explorer Tree and drag it into the box.  At least one of the variables selected should be a continuous variable (ex. Age) and one should be a categorical variable (ex. Tumor Stage).  A continuous variable can be categorized using the binning option below.					
				</td>					
			</tr>
	
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupBox('divIndependentVariable')" value="X"> <br />
					<div id='divIndependentVariable' class="queryGroupIncludeLong"></div>
				</td>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupBox('divDependentVariable')" value="X"> <br />
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
					<span class='AnalysisHeader'>Binning&nbsp;&nbsp;&nbsp;<input id="BinningToggle" type="button"
							value="Enable" onClick="toggleBinning();" /></span></td>
			</tr>
			<tr>
				<td>
					<hr />
				</td>
			</tr>
		</table>

		<div id="divBinning" style="display: none;">

			<table id="tblBinningTable" class="subsettable"
				style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
				<tr>
					<td align="left"><b>Variable</b> : <select id="selBinVariableSelection">
							<option value="IND">Independent</option>
							<option value="DEP">Dependent</option>
					</select></td>
				</tr>
				<tr>
					<td><b>Variable Type</b> <select id="variableType"
						onChange="updateManualBinning();">
							<option value="Continuous">Continuous</option>
							<option value="Categorical">Categorical</option>
					</select></td>
				</tr>
				<tr>
					<td align="left"><b>Number of Bins</b> : <input type="text"
						id="txtNumberOfBins" onChange="manageBins(this.value);" value="4" />
					</td>
				</tr>
				<tr>
					<td align="left"><b>Bin Assignments (Continuous variables
							only)</b> : <select id="selBinDistribution">
							<option value="EDP">Evenly Distribute Population</option>
							<option value="ESB">Evenly Spaced Bins</option>
					</select></td>
				</tr>
				<tr>
					<td align="left">&nbsp;</td>
				</tr>
				<tr>
					<td align="left"><b>Manual Binning</b> : <input
						type="checkbox" id="chkManualBin" onClick="manageBins(document.getElementById('txtNumberOfBins').value);" />
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
							<table style="border: 0px none; border-collapse: collapse;">
								<tr>
									<td style="vertical-align: top;">
										<b>Categories</b>
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
		
		<table class="subsettable" style="margin: 10px;width:530px; border: 0px none; border-collapse: collapse;">
			<tr>
				<td align="center">
					<input type="button" value="Run" onClick="submitBoxPlotJob(this.form);">
				</td>
			</tr>
		</table>		
		
	</form>