<r:require modules="smartR_all"/>

<div id="sr-loading">Loading...</div>
<div id="sr-index" data-ng-app="smartRApp" style="padding: 10px;">
    <h1 style="font-size: 14px">SmartR - Dynamic Data Visualization and Interaction</h1>
    <br>

    <div align="left">
        <g:select name="sr-workflowSelect"
                  from="${scriptList}"
                  noSelection="${['': 'Please select a workflow']}"
                  optionValue="${{it.capitalize() + ' Workflow'}}"
                  ng-model="template"
                  onchange="cleanUpSmartR()"/>
        <button id="sr-reset-btn" ng-click="template=''" onclick="cleanUpSmartR()">Reset SmartR</button>
    </div>

    <div style="width: 50%; margin: 0 auto; text-align: center">
        <cohort-summary-info></cohort-summary-info>
    </div>

    <hr class="sr-divider">

    <ng-include src="template"></ng-include>

    <g:each in="${scriptList}">
        <g:render template="/layouts/${it}"/>
    </g:each>
</div>

