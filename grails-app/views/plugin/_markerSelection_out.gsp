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
<link rel="stylesheet" type="text/css" href="${resource(dir:'css', file:'datasetExplorer.css')}">

</head>

<body>
	<form>
		<span class='AnalysisHeader'>Marker Selection - Heatmap</span><br />
		<i>Click on the heatmap image to open it in a new window as this may increase readability.</i>
		<br />
		<br />
		<g:each var="location" in="${imageLocations}">
	    	<a onclick="window.open('${location}','_blank')"><img src='${location}'  width="600" height="600"/></a> <br />
		</g:each>
		
		<br />
		<span class='AnalysisHeader'>Table of top Markers</span>
		<g:if test="${grailsApplication.config.com.thomsonreuters.transmart.metacoreAnalyticsEnable}">
			&nbsp;<g:metacoreSettingsButton /><input type="button" value="Run MetaCore Enrichment Analysis" onClick="markerSelectionRunMetacoreEnrichment();" />
		</g:if>
		<br />
		<g:if test="${grailsApplication.config.com.thomsonreuters.transmart.metacoreAnalyticsEnable}">
			<g:render template="/metacoreEnrichment/enrichmentResult" model="[prefix: 'marker_']"/>
		</g:if>
		<br />
		
		${markerSelectionTable}
		
		<br />
		<a class='AnalysisLink' href="${zipLink}">Download raw R data</a>
	</form>
</body>

</html>
