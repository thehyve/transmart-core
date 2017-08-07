
	<g:select style="width: 400px" id="measurement" name="measurement" noSelection="${['null':'Select...']}" from="${measurements}" value="${measurement}"
			onchange="${legacy.remoteFunction(action:'ajaxTechnologies', update: [success: 'technologywrapper'], onSuccess:
					'updatePlatforms()', params:'\'measurementName=\' + this.value + \'&technologyName=\' + \n' +
					'document.getElementById(\'technology\').value + \'&vendorName=\' + document.getElementById(\'vendor\').value' )};
					${legacy.remoteFunction(action:'ajaxVendors', update: [success: 'vendorwrapper'], onSuccess:
					'updatePlatforms()',
					params:'\'measurementName=\' + this.value + \'&technologyName=\' + document.getElementById(\'technology\').value + \'&vendorName=\' + document.getElementById(\'vendor\').value' )}"/>
