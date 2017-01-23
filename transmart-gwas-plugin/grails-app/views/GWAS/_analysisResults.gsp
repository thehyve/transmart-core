
	<g:if test="${wasRegionFiltered}">
		<div class="infobox">These results have been filtered according to gene/chromosome region criteria.</div>
	</g:if>
	<div id="analysis_results_table_${analysisId}_length_wrapper" class="dataTables_length">
		<label>Show 
			<g:select name="resultCounter" size="1" from="${['10':'10','25':'25','50':'50','100':'100']}" optionKey="${{it.key}}" optionValue="${{it.value}}" id="analysis_results_table_${analysisId}_length" onchange="loadAnalysisResultsGrid(${analysisId}, {max: jQuery(this).val()})" value="${max}"/> entries
		</label>	
	</div>
	<div class="dataTables_filter">
		<label>P-value cutoff: 
			<input type="text" style="width: 50px" id="analysis_results_table_${analysisId}_cutoff" onclick="removepvalue(${analysisId})"> <%-- removed value="${cutoff}" --%>
		</label>
		<label>Search: 
			<input value="${search}" type="text" style="width: 50px" id="analysis_results_table_${analysisId}_search">
			<span class="linkbutton" onclick="loadAnalysisResultsGrid(${analysisId}, {cutoff: jQuery('#analysis_results_table_${analysisId}_cutoff').val(), search: jQuery('#analysis_results_table_${analysisId}_search').val()})">OK</span>
		</label>
	</div>
	<table id="analysis_results_table_${analysisId}" class="dataTable">
		<thead>
			<tr role="row">
				<g:each in="${columnNames}" var="col">
					<g:unless test="${col.sTitle == 'Analysis'}"> <%-- Skip analysis column for analysis results - there can be only one --%>
						<g:if test="${col.sortField}">
							<%-- If this is sorted, give link to asc/desc (opposite current order) --%>
							<g:if test="${col.sortField.equals(sortField)}">
								<g:if test="${order.equals('desc')}">
									<th "class="sorting" tabindex="0" rowspan="1" colspan="1" style="width: 100px; cursor: pointer;" onclick="loadAnalysisResultsGrid(${analysisId}, {sortField: '${col.sortField}', order: 'asc'})">
										${col.sTitle} <img src="${resource([file:'desc.gif', dir:'images'])}"/>
									</th>
								</g:if>
								<g:else>
									<th "class="sorting" tabindex="0" rowspan="1" colspan="1" style="width: 100px; cursor: pointer;" onclick="loadAnalysisResultsGrid(${analysisId}, {sortField: '${col.sortField}', order: 'desc'})">
										${col.sTitle} <img src="${resource([file:'asc.gif', dir:'images'])}"/>
									</th>
								</g:else>
							</g:if>
							<%-- Otherwise just provide asc link --%>
							<g:else>
								<th "class="sorting" tabindex="0" rowspan="1" colspan="1" style="width: 100px; cursor: pointer;" onclick="loadAnalysisResultsGrid(${analysisId}, {sortField: '${col.sortField}', order: 'asc'})">
									${col.sTitle}
								</th>
							</g:else>
						</g:if>
						<g:else>
							<th class="sorting" tabindex="0" rowspan="1" colspan="1" style="width: 100px; ">${col.sTitle}</th>
						</g:else>
					</g:unless>
				</g:each>
			</tr>
		</thead>
		<tbody role="alert" aria-live="polite" aria-relevant="all">
			
				<g:each in="${analysisData}" var="row" status="i">
					<g:set var="link" value="http://regulomedb.org/snp/chr${row[5]}/${row[6]}" />
					<tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
					<g:each in="${row}" var="data" status="colNum">
						<g:unless test="${colNum == 0}"> <%-- Skip analysis name --%>
							<g:if test="${colNum == 1}">
								<td class=""><a href="${link}" target="_blank"><span class="filter-item filter-item-gene">${data}</span></a>
								<img alt="" src="${resource(dir:'images',file:'linkext7.gif')}" />
							</g:if>
							<g:else>
								<td class="">${data}</td>
							</g:else>
						</g:unless>
					</g:each>
					</tr>
				</g:each>
			
		</tbody>
	</table>						
	<div class="dataTables_info" id="analysis_results_table_${analysisId}_info">
		<g:if test="${totalCount > 0}">
			Showing ${Math.min(offset+1, totalCount)} to ${Math.min(totalCount,offset+max)} of${wasShortcut?' about ':' '}${totalCount} entries
		</g:if>
		<g:else>
			No entries to display
		</g:else>
	</div>
	<div class="dataTables_paginate paging_two_button" id="analysis_results_table_${analysisId}_paginate">
		<g:if test="${offset > 0}">
			<a class="paginate_enabled_previous" tabindex="0" role="button" id="analysis_results_table_${analysisId}_previous" onclick="loadAnalysisResultsGrid(${analysisId}, {offset: ${offset-max}})">Previous</a>
		</g:if>
		<g:else>
			<a class="paginate_disabled_previous" tabindex="0" role="button" id="analysis_results_table_${analysisId}_previous">Previous</a>
		</g:else>
		
		<g:if test="${offset + max < totalCount}">
			<a class="paginate_enabled_next" tabindex="0" role="button" id="analysis_results_table_${analysisId}_next" onclick="loadAnalysisResultsGrid(${analysisId}, {offset: ${offset+max}})">Next</a>
		</g:if>
		<g:else>
			<a class="paginate_disabled_next" tabindex="0" role="button" id="analysis_results_table_${analysisId}_next">Next</a>
		</g:else>
	</div>