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
		<br /><br />		
		
		<span class='AnalysisHeader'>Line Graph</span>
		
		<br /><br />	

		<g:each var="location" in="${imageLocations}">
            <g:img file="${location}" width="600" height="600"></g:img> <br />
		</g:each>
		
		<br /><br />

        <a class='AnalysisLink' href="${resource(file: zipLink)}">Download raw R data</a>
		
	</form>
</body>

</html>