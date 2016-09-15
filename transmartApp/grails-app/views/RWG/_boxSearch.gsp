<g:if test="${hide}"><g:set var="csshide">display: none</g:set></g:if>

<div id="box-search" style="${csshide}">
    <div id="title-search-div" class="ui-widget-header boxtitle">
        <h2 style="float:left" class="title">
            Active Filters
            <g:if test="${!globalOperator}">
                <g:set var="globalOperator" value="and"/>
            </g:if>
            <div id="globaloperator" class="andor ${globalOperator.toLowerCase()}">&nbsp;</div>
        </h2>

        <div id="clearbutton" class="greybutton filterbrowser">
            <a href="#" onclick="clearSearch(); return false;">Clear</a>
        </div>
        <div id="filterbutton" class="greybutton filterbrowser">
            <asset:image src="filter.png"/> Filter
        </div>
    </div>

    <div id="active-search-div" class="boxcontent">
        &nbsp;
    </div>
</div>