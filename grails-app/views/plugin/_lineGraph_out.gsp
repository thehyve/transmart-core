<h2>Line Graph</h2>

<p>
    <g:each var="location" in="${imageLocations}">
        <g:img file="${location}" class="img-result-size"></g:img> <br/>
    </g:each>

    <div>
        <a class='AnalysisLink' href="${resource(file: zipLink)}">Download raw R data</a>
    </div>
</p>
