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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
  <head>
    <title>subsetPanel.html</title>
	
    <meta http-equiv="keywords" content="keyword1,keyword2,keyword3"/>
    <meta http-equiv="description" content="this is my page"/>
    <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1"/>
    <link rel="stylesheet" type="text/css" href="${resource(dir:pluginContextPath, file:'css/dataAssociation.css')}"/>
  </head>
  
  <body>
   <div id="toolbar"></div>
   <div id="dataAssociationBody">
  	<div style="text-align:left;font:12pt arial;">
  		<table class="subsettable" style="margin: 10px; width: 90%; border: 0px none; border-collapse: collapse;">
			<tr>
				<td>
					<span class='AnalysisHeader'>Cohorts</span>
				</td>
				<td align="right">
					<input type="button" value="Save To PDF" onclick="javascript: generatePdfFromHTML('dataAssociationBody', 'DataAssociation.pdf');"></input>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div id = "cohortSummary"></div>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<hr></hr>
				</td>
			</tr>
  			<tr>
  				<td colspan="2"><span class='AnalysisHeader'>Analysis: </span>
  				<span id="selectedAnalysis">Select Analysis from the "Analysis" menu</span>
  				<input type="hidden" id="analysis" name="analysis" />
  				
				<a href='JavaScript:D2H_ShowHelp(1503,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
			<img src="${resource(dir:'images',file:'help/helpicon_white.jpg')}" alt="Help" border=0 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
				</a>	  				
  				</td>
			</tr>
			<tr>
				<td colspan="2">
					<hr></hr>
				</td>
			</tr>
		</table>
	</div>
	<div id="variableSelection"></div>
	<div style="page-break-after:always"></div>
	<div id="analysisOutput" style="margin:10px;"></div>
   </div>
  </body>
  
</html>