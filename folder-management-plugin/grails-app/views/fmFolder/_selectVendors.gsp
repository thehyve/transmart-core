
<g:select style="width: 400px" id="vendor" name="vendor" noSelection="${['null':'Select...']}" from="${vendors}" value="${vendor}"
			onchange="${legacy.remoteFunction(action:'ajaxTechnologies', update: 'technologywrapper', onSuccess:
					'updatePlatforms()',
					params:'\'vendorName=\' + this.value + \'&technologyName=\' + document.getElementById(\'technology\').value + \'&measurementName=\' + document.getElementById(\'measurement\').value' )};
					${legacy.remoteFunction(action:'ajaxMeasurements', update: 'measurementwrapper', onSuccess:
					'updatePlatforms()',
					params:'\'vendorName=\' + this.value + \'&technologyName=\' + document.getElementById(\'technology\').value + \'&measurementName=\' + document.getElementById(\'measurement\').value' )}"/>
