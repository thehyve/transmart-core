/*************************************************************************   
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

function submitLineGraphJob(form){
	var dependentVariableEle = Ext.get("divDependentVariable");
	var dependentVariableConceptCode = getQuerySummaryItem(dependentVariableEle.dom.childNodes[0]);

	var independentVariableEle = Ext.get("divIndependentVariable");
	var independentVariableConceptCode = getQuerySummaryItem(independentVariableEle.dom.childNodes[0]);
	
	var groupByVariableEle = Ext.get("divGroupByVariable");
	var groupByVariableConceptCode = getQuerySummaryItem(groupByVariableEle.dom.childNodes[0]);
	
	var formParams = {dependentVariable:dependentVariableConceptCode,
						independentVariable:independentVariableConceptCode,
						groupByVariable:groupByVariableConceptCode};
	
	submitJob(formParams);
}

function loadLineGraphView(){
	registerLineGraphDragAndDrop();
}

function registerLineGraphDragAndDrop()
{
	//Set up drag and drop for Dependent and Independent variables on the data association tab.
	//Get the Dependent DIV.
	var dependentDiv = Ext.get("divDependentVariable");
	//Get the Independent DIV
	var independentDiv = Ext.get("divIndependentVariable");
	//Get the group by div
	var groupByDiv = Ext.get("divGroupByVariable");
	
	//Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(dependentDiv,{ddGroup : 'makeQuery'});
	dtgD.notifyDrop =  dropOntoVariableSelection;
	
	dtgI = new Ext.dd.DropTarget(independentDiv,{ddGroup : 'makeQuery'});
	dtgI.notifyDrop =  dropOntoVariableSelection;
	
	dtgG = new Ext.dd.DropTarget(groupByDiv, {ddGroup: 'makeQuery'});
	dtgG.notifyDrop = dropOntoVariableSelection;
}