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
<link rel="stylesheet" type="text/css"
	href="${resource(dir:'css', file:'datasetExplorer.css')}">

</head>

<body>
	<form>
		<table>
			<tr>
				<th>
					<span class='AnalysisHeader'>
						Logistic Regression
						
						<a href='JavaScript:D2H_ShowHelp(1350,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
							<img src="${resource(dir:'images',file:'help/helpicon_white.jpg')}" alt="Help" border=0 width=18pt style="margin-top:1pt;margin-bottom:1pt;margin-right:18pt;"/>
						</a>			
					</span>
				</th> 
			</tr>
			<tr>
				<g:each var="location" in="${imageLocations}">
			    	%{--<img src='${location}'  width="700" height="700" alt="Logistic Regression"/> <br />--}%
                    <g:img file="${location}" width="700" height="700" ></g:img> <br />
				</g:each>
			</tr>				
			<tr>
				<td>
					${LOGREGData}		
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;
				</td>
			</tr>				
			<tr>
				<td>
					<g:if test="${zipLocation}">
						<a class='AnalysisLink' class='downloadLink' href="${resource(file: zipLocation)}">Download raw R data</a>
					</g:if>
				</td>
			</tr>		
			<tr>
				<td>
					&nbsp;
				</td>
			</tr>	
			<tr>
				<td>
					${rVersionInfo}
				</td>
			</tr>								
		</table>
	</form>
</body>



</html>



