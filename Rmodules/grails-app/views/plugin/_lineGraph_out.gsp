<h2>Line Graph</h2>

<p>
    <g:each var="location" in="${imageLocations}">
        <img src="${location}"/> <br/>
    </g:each>

    <g:if test="${zipLink}">
        <a class='AnalysisLink' class='downloadLink' href="${zipLink}">Download raw R data</a>
    </g:if>
</p>
