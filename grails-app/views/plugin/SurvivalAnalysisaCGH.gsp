%{--Survival Analysis aCGH input Container--}%
<div id="analysisContainer"></div>

%{--Survival Analysis aCGH toolbar--}%
<div id="analysisToolBar"></div>

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Survival Analysis aCGH toolbar--}%
<div id="plotResultWrapper">

</div>

%{--==========================--}%
%{--Template for Survival Plot--}%
%{--==========================--}%

<extjs-tpl id="template-survival-plot" class="x-hidden">
	<div id="plotResultContainer" class="plotResultContainer">
		<h1>{region} {alteration}</h1>

		<div id="plotBody" class="plotBody">
			<div class="leftDiv">
				<g:img dir="images/tempImages/{foldername}" file="{filename}" height='350'
				       width='350'></g:img>
			</div>

			<div class="bttDiv">
				Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec aliquam faucibus tortor, eget consequat
				ipsum pharetra nec. Pellentesque molestie condimentum eros tempor imperdiet. Sed ac eros a lorem vehicula
				hendrerit. Curabitur vel congue risus. Nullam ut elit vitae felis gravida fringilla at eu lectus.
				Etiam adipiscing luctus libero, sit amet ultricies mi pretium sit amet. Duis faucibus, sem id eleifend
				aliquam, tellus diam eleifend metus, sed accumsan turpis massa id erat. Morbi laoreet est at est sodales ac
				viverra est lacinia. Curabitur lacinia rutrum ligula eu malesuada. Lorem ipsum dolor sit amet, consectetur
				adipiscing elit. Vivamus viverra odio vitae odio sodales consequat. Nam a est vestibulum dui pharetra
				mattis. Fusce eu libero mauris. Aliquam consequat tincidunt arcu. Integer vel congue odio.
			</div>
		</div>
	</div>
</extjs-tpl>
