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
	
		<table class="subsettable" style="margin: 10px;width:300px; border: 0px none; border-collapse: collapse;" >
			<tr>
				<td>
					<span class='AnalysisHeader'>Variable Selection</span>
					<a href='JavaScript:D2H_ShowHelp(1513,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
				<img src="${resource(dir:'images',file:'help/helpicon_white.jpg')}" alt="Help" border=0 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
					</a>					
				</td>			
			</tr>	
			<tr>
				<td>
					<hr />
				</td>
			</tr>	
			<tr>
				<td align="center">
					<span class='AnalysisHeader'>Data Node</span>
					<br />
					<br />
					Select a continuous variable from the Data Set Explorer Tree and drag it into the box.
				</td>					
			</tr>
	
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroupWaterfall('divDataNode')" value="X"> 
					<br />
					<div id='divDataNode' class="queryGroupIncludeSmall"></div>
				</td>
			</tr>
			<!-- 
			<tr>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="gatherHighDimensionalData('divDataNode')" value="High Dimensional Data">
				</td>
			</tr>
			 
			<tr><td><br/></td></tr>
			<tr>
				<td>
					<div id="displaydivDataNode"></div>
				</td>
			</tr>
			-->
			<tr><td><br/></td></tr>
			<tr>
				<td>
					Low Range 
					<select id="selLowRange">
						<option value="&lt;" selected="selected">&lt;</option>
						<option value="&lt;=">&lt;=</option>
						<option value="=">=</option>
						<option value="&gt;">&gt;</option>
						<option value="&gt;=">&gt;=</option>
					</select>
					<input id="txtLowRange">
				</td>
			</tr>
			<tr>
				<td>
					High Range 
					<select id="selHighRange">
						<option value="&gt;" selected="selected">&gt;</option>
						<option value="&gt;=">&gt;=</option>					
						<option value="&lt;">&lt;</option>
						<option value="&lt;=">&lt;=</option>
						<option value="=">=</option>
					</select>
					<input id="txtHighRange">
				</td>
			</tr>			
			<tr><td><br/></td></tr>
			<tr>
				<td align="center">
					<input type="button" value="Run" onClick="submitWaterfallJob(this.form);"></input>
					
					<input type="button" value="Select inputs as Cohort" onClick="selectInputsAsCohort(this.form);"></input>
				</td>
			</tr>
		</table>
	</form>
</body>

</html>