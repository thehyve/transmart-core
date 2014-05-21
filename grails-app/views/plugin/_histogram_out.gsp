<h2>Done</h2>

<p>
    <g:each var="location" in="${imageLocations}">
        <a onclick="window.open('${resource(file: location, dir: "images")}', '_blank')">
            <g:img file="${location}" width="600" height="600"></g:img>
        </a>
    </g:each>

    <div>
        <a href="${resource(file: zipLink)}" class="downloadLink">Download raw data</a>
    </div>
</p>

