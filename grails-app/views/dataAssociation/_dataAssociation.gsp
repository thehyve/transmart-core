%{--Toolbar--}%
<div id="toolbar"></div>

%{-- Data Association Content --}%
<div id="dataAssociationBody" class="snowpanel">

	%{-- Variable Selection --}%
	<div class="subsettable" >

		    %{--display selected analysis--}%
			<label for="selectedAnalysis">
				Analysis:
				<span id="selectedAnalysis" class="warning">WARNING: Analysis is not selected</span>

				%{-- TBD: help hyperlink
				<a href='JavaScript:D2H_ShowHelp(1503,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
					<img src="${resource(dir: 'images', file: 'help/helpicon_white.jpg')}" alt="Help" border=0
						 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
				</a>--}%
	
				<input type="hidden" id="analysis" name="analysis"/>
			</label>
			<hr style="height: 1px;"/>
			%{--display selected cohort--}%
			<label for="cohortSummary">
				Cohorts:
				<span id="cohortWarningMsg" class="warning"></span>
				<div id="cohortSummary"></div>
			</label>
	</div>
  
</div>
  				
	%{-- Variable Selection --}%
	<div id="variableSelection"></div>

	%{-- Page Break for PDF--}%
	<div style="page-break-after:always"></div>
  
	%{-- Analysis Output --}%
	<div id="analysisOutput" ></div>
