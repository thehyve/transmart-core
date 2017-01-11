<h2>Heatmap</h2>

<p>
    <div class="plot_hint">
        Click on the heatmap image to open it in a new window as this may increase readability.
        <br><br>
    </div>

    <g:each var="location" in="${imageLocations}">
        <a onclick="window.open('${location}', '_blank')">
            <img src="${location}" class="img-result-size"/> <br/>
        </a>
    </g:each>

    <g:if test="${zipLink}">
        <a class='AnalysisLink' class='downloadLink' href="${zipLink}">Download raw R data</a>
    </g:if>
</p>

