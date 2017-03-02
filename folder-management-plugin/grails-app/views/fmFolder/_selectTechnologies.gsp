
<g:select style="width: 400px" id="technology" name="technology" noSelection="${['null':'Select...']}" from="${technologies}" value="${technology}"
				onchange="${legacy.remoteFunction(action:'ajaxMeasurements', update: [success: 'measurementwrapper'], onSuccess:
						'updatePlatforms()',
						params:'\'technologyName=\' + this.value + \'&vendorName=\' + document.getElementById(\'vendor\').value + \'&measurementName=\' + document.getElementById(\'measurement\').value' )};
					${legacy.remoteFunction(action:'ajaxVendors', update: [success: 'vendorwrapper'], onSuccess:
						'updatePlatforms()', params:'\'technologyName=\' + this.value + \'&vendorName=\' + document.getElementById(\'vendor\').value + \'&measurementName=\' + document.getElementById(\'measurement\').value' )}"/>
