<h2>Marker Selection - Heatmap</h2>

<p>
    <div class="plot_hint">
        Click on the heatmap image to open it in a new window as this may increase readability.<br><br>
    </div>

    <g:each var="location" in="${imageLocations}">
        <a onclick="window.open('${location}', '_blank')">
            <img src="${location}" class="img-result-size"/> <br/>
        </a>
    </g:each>

    <div>
        <span class='AnalysisHeader'>Table of top Markers</span>
        <g:if test="${grailsApplication.config.com.thomsonreuters.transmart.metacoreAnalyticsEnable}">
            &nbsp;<g:metacoreSettingsButton /><input type="button" value="Run MetaCore Enrichment Analysis"
                                                     onClick="markerSelectionRunMetacoreEnrichment();" />
        </g:if>
        <br />
        <g:if test="${grailsApplication.config.com.thomsonreuters.transmart.metacoreAnalyticsEnable}">
            <g:metacoreEnrichmentResult/>
        </g:if>
    </div>

    ${raw(markerSelectionTable)}

    <g:if test="${zipLink}">
        <a class='AnalysisLink' class='downloadLink' href="${zipLink}">Download raw R data</a>
    </g:if>
</p>
