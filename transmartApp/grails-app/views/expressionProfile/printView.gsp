<!DOCTYPE HTML>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <asset:link rel="shortcut icon" href='searchtool.ico' type="image/x-ico" />
    <asset:link rel="icon" href='searchtool.ico' type="image/x-ico" />
    <asset:stylesheet href="main.css">
    <title>${grailsApplication.config.com.recomdata.appTitle}</title>
</head>

<body onload="window.print();">
<table>
    <tr><td><asset:image src="${createLink(action: 'displayChart') + '?filename=' + filename}"/></td></tr>
    <tr><td>&nbsp;</td></tr>
    <tr><td><center>
        <a href="#" onclick="window.print();">
            <asset:image src="print.png"/>
            Print
        </a>
    </center></td></tr>
</table>
</body>
</html>
