<!DOCTYPE html>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js"><!--<![endif]-->
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<title><g:layoutTitle default="Grails"/></title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<asset:link rel="shortcut icon" href='favicon.ico' type="image/x-ico" />
		<asset:link rel="apple-touch-icon" href='apple-touch-icon.png' />
		<asset:link rel="apple-touch-icon-retina" sizes="114x114" href='apple-touch-icon-retina.png' />
		<asset:stylesheet href="main.css"/>
		<asset:stylesheet href="mobile.css"/>
		<g:layoutHead/>
		%{--<g:javascript library="application"/>		--}%
		%{--<r:layoutResources />--}%
	</head>
	<body>
		<div id="grailsLogo" role="banner"><a href="http://grails.org"><asset:image src="grails_logo.png" alt="Grails"/></a></div>
		<g:layoutBody/>
		<div class="footer" role="contentinfo"></div>
		<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
		%{--<r:layoutResources />--}%
	</body>
</html>
