%{--Dummy--}%
<p>DONE ... </p>

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Plot wrapper--}%
<div id="plotResultWrapper"></div>

%{--==========================--}%
%{--Template for Survival Plot--}%
%{--==========================--}%

<extjs-tpl id="template-survival-plot" class="x-hidden">
	<div id="plotResultContainer" class="plotResultContainer">
		<h1>Chr: {chromosome} ({start}-{end})</h1>

		<div id="plotBody" class="plotBody">
			<div id="plotCurve">
				<g:img dir="images/tempImages/{foldername}" file="{filename}" height='350' width='350'></g:img>
			</div>

			<div id="plotCurveDesc">
				<table id="newspaper-a">
					<caption>Table: Selected region details.</caption>
					<thead>
					<tr>
						<th scope="col">Chromosome</th>
						<th scope="col">start</th>
						<th scope="col">end</th>
						<th scope="col">p-value</th>
						<th scope="col">fdr</th>
					</tr>
					</thead>
					<tfoot>
					<tr>
						<td colspan="5">
							<em></em>
						</td>
					</tr>
					</tfoot>
					<tbody>
					<tr>
						<td>{chromosome}</td>
						<td>{start}</td>
						<td>{end}</td>
						<td>{pvalue}</td>
						<td>{fdr}</td>
					</tr>
					</tbody>
				</table>
			</div>
		</div>

		<div id="{survivalDownloadBtn}" class="downloadBtnInnerPage"></div>
	</div>
</extjs-tpl>
