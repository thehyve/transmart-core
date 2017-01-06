<h2>Component Summary</h2>

${raw(summaryTable)}

<br />

<g:each var="location" in="${imageLocations}">
    <g:img file="${location}"></g:img> <br />
</g:each>

<br />

<h2>Gene list by proximity to Component</h2>

${raw(geneListTable)}

<br />
<g:if test="${zipLink}">
    <a class='AnalysisLink' class='downloadLink' href="${resource(file: zipLink)}">Download raw R data</a>
</g:if>
