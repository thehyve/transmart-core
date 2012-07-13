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
	
		<table class="subsettable" style="margin: 10px; border: 0px none; border-collapse: collapse;">
			<tr>
				<td colspan="5">
					<h1>Variable Selection</h1>
				</td>			
			</tr>	
			<tr>
				<td colspan="5">
					<hr />
				</td>
			</tr>	
			<tr>
				<td align="center">
					<h2>
						<u>Independent Variable</u>
					</h2>
				</td>
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<h2>
						<u>Dependent Variable</u>
					</h2>
				</td>
				<td id="subsetdivider" rowspan="21" valign="center" align="center" height="100%">
					<div style="margin: 15px; border: 1px solid black; background: black; width: 1px; height: 150px"></div>
				</td>
				<td align="center">
					<h2>
						<u>Group by</u>
					</h2>
				</td>										
			</tr>
	
			<tr>
				<td align="right">
					<button style="font: 9pt tahoma;" onclick="clearGroup('divDependentVariable')">X</button> <br />
					<div id='divDependentVariable' class="queryGroupIncludeSmall"></div>
				</td>
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroup('divIndependentVariable')" value="X"> <br />
					<div id='divIndependentVariable' class="queryGroupIncludeSmall"></div>
				</td>			
				<td align="right">
					<input style="font: 9pt tahoma;" type="button" onclick="clearGroup('divGroupByVariable')" value="X"> <br />
					<div id='divGroupByVariable' class="queryGroupIncludeSmall"></div>
				</td>
			</tr>
			<tr>
				<td>
					<input type="button" value="Run" onClick="submitLineGraphJob(this.form);"></input>
				</td>
			</tr>
		</table>
	</form>
</body>

</html>