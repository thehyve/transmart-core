
<g:if test="${!subResourcesAssayMultiMap && !tags && !browseStudyInfo && !dataTypes}">
    <g:message code="show.definition.noInfo" default="No Information Found"/>
</g:if>
<g:else>
    <style>
    tr.prop > td.name {
        width: 150px;
    }
    </style>
    <g:if test="${subResourcesAssayMultiMap}">
        <g:render template="highDimSummary" model="subResourcesAssayMultiMap"/>
    </g:if>
    <g:if test="${tags}">
        <g:render template="showTags" model="tags"/>
    </g:if>
    <g:if test="${dataTypes}">
        <g:render template="showDataTypes" model="dataTypes"/>
    </g:if>
</g:else>
<g:if test="${grailsApplication.config.requestStudyAccessUrl && !hasAccess}">
    <br/>
    <h2>Access:</h2>
    <a href="${ grailsApplication.config.requestStudyAccessUrl
            .replaceAll("\\{studyId\\}", studyId)
            .replaceAll("\\{studyName\\}", studyName)
            .replaceAll("\\{userId\\}", userId.toString())
            .replaceAll("\\{userName\\}", userName)}"
       target="_blank">Request access</a>
</g:if>
