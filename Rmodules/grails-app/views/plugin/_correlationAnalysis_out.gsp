<h2>Correlation Table (p-values on top right half, correlation coefficient on bottom left)</h2>

<p>
    <p>${raw(correlationData)}</p>
    <g:each var="location" in="${imageLocations}">
        <img src="${location}"/>
    </g:each>



    <g:if test="${zipLink}">
        <a class='AnalysisLink' class='downloadLink' href="${zipLink}">Download raw R data</a>
    </g:if>
</p>
