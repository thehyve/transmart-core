%{--include js lib for heatmap dynamically--}%
<r:require modules="marker_selection"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1508,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
            <img src="${resource(dir: 'images', file: 'help/helpicon_white.jpg')}" alt="Help"/>
        </a>
    </h2>

    <form id="analysisForm">
        <fieldset>

            %{--High dimensional input--}%
            <div class="highDimContainer">
                <span>Select a High Dimensional Data node from the Data Set Explorer Tree and drag it into the box.</span>
                <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                <div id="highDimBtns">
                    <button type="button" onclick="gatherHighDimensionalData('divIndependentVariable')">High Dimensional Data</button>
                    <input type="hidden" id="multipleSubsets" name="multipleSubsets" value="true" />
                    <button type="button" onclick="clearGroupMarkerSelection('divIndependentVariable')">Clear</button>
                </div>
            </div>

            %{--Display independent variable--}%
            <div id="displaydivIndependentVariable" class="independentVars"></div>

            <label for="txtNumberOfMarkers">Number of Markers:</label>
            <input type="text" id="txtNumberOfMarkers" value="50"/>

            <label for="txtImageWidth">Image Width (pixels):</label>
            <input type="text" id="txtImageWidth" value="800"/>

            <label>Image Height (pixels):</label>
            <input type="text" id="txtImageHeight" value="800"/>

            <label>Text Size (pointsize):</label>
            <input type="text" id="txtImagePointsize" value="12"/>

            <div>
                <input type="button" value="Run" onClick="submitMarkerSelectionJob(this.form);" class="runAnalysisBtn">
            </div>

        </fieldset>
    </form>

</div>

%{--
	<form>
	
		<table class="subsettable" style="margin: 10px;width:300px; border: 0px none; border-collapse: collapse;" >
			<tr>
				<td colspan="4">
					<span class='AnalysisHeader'>Variable Selection</span>
					<a href='JavaScript:D2H_ShowHelp(1508,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
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
					<span class='AnalysisHeader'>Marker Variable</span>
					<br />
					<br />
					Select a High Dimensional Data node from the Data Set Explorer Tree and drag it into the box.
				</td>
			</tr>
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupMarkerSelection('divIndependentVariable')" value="X"> 
					<br />
					<div id='divIndependentVariable' class="queryGroupIncludeSmall"></div>
				</td>
			</tr>
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divIndependentVariable')" value="High Dimensional Data">
					<input type="hidden" id="multipleSubsets" name="multipleSubsets" value="true" />
				</td>
			</tr>
			<tr><td><br/></td></tr>
			<tr>
				<td>
					Number of Markers : <input id="txtNumberOfMarkers" name="txtNumberOfMarkers" value="50" />
				</td>
			</tr>			
			<tr><td><br/></td></tr>
			<tr>
				<td>
					<div id = "displaydivIndependentVariable"></div>
				</td>
			</tr>
			<tr><td><br/></td></tr>			
			<tr>
				<td align="center">
					Image Width (pixels) :  <input id="txtImageWidth" value="1200" />					
					<br />
					<br />
				</td>
			</tr>				
			<tr><td><br/></td></tr>
			<tr>
				<td align="center">
					Image Height (pixels) :  <input id="txtImageHeight" value="800" />					
					<br />
					<br />
				</td>
			</tr>				
			<tr><td><br/></td></tr>
			<tr>
				<td align="center">
					Text size (pointsize) :  <input id="txtImagePointsize" value="12" />					
					<br />
					<br />
				</td>
			</tr>							
			<tr><td><br/></td></tr>
			<tr>
				<td colspan="4" align="center">
					<input type="button" value="Run" onClick="submitMarkerSelectionJob(this.form);">
				</td>
			</tr>
		</table>
	</form>
  --}%