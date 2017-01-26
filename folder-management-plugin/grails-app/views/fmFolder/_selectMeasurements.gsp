
	<g:select style="width: 400px" id="measurement" name="measurement" noSelection="${['null':'Select...']}" from="${measurements}" value="${measurement}"
			onchange="${legacy.remoteFunction(action:'ajaxTechnologies', update: 'technologywrapper', onSuccess:
					'updatePlatforms()', params:'\'measurementName=\' + this.value + \'&technologyName=\' + \n' +
					'document.getElementById(\'technology\').value + \'&vendorName=\' + document.getElementById(\'vendor\').value' )};
					${legacy.remoteFunction(action:'ajaxVendors', update: 'vendorwrapper', onSuccess:
					'updatePlatforms()',
					params:'\'measurementName=\' + this.value + \'&technologyName=\' + document.getElementById(\'technology\').value + \'&vendorName=\' + document.getElementById(\'vendor\').value' )}"/>
