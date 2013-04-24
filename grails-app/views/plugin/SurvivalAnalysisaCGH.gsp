%{--Survival Analysis aCGH input Container--}%
<div id="analysisContainer"></div>

%{--Survival Analysis aCGH toolbar--}%
%{--<div id="analysisToolBar"></div>--}%

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Survival Analysis aCGH toolbar--}%
<div id="plotResultWrapper"></div>

%{--==========================--}%
%{--Template for Survival Plot--}%
%{--==========================--}%

<extjs-tpl id="template-survival-plot" class="x-hidden">
	<div id="plotResultContainer" class="plotResultContainer">
		<h1>Region: {region} ({cytoband})</h1>

		<div id="plotBody" class="plotBody">
			<div id="plotCurve">
				<g:img dir="images/tempImages/{foldername}" file="{filename}" height='350' width='350'></g:img>
			</div>

			<div id="plotCurveDesc">
				<table id="newspaper-a">
					<caption>Table: Selected region details.</caption>
					<thead>
						<tr>
							<th scope="col">Region</th>
							<th scope="col">Cytoband</th>
							<th scope="col">p-value</th>
							<th scope="col">fdr</th>
							<th scope="col">Alteration</th>
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
							<td>{region}</td>
							<td>{cytoband}</td>
							<td>{pvalue}</td>
							<td>{fdr}</td>
							<td>{alteration}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>

		<div id="{plotCurveToolBarId}" class="plotCurveToolBar"></div>
	</div>
</extjs-tpl>
