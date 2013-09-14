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

package com.recomdata.transmart.data.association.pdf

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * PDFController handles generating PDF file from HTML content.
 * If you need to generate PDF from a GSP page refer to Grails Plugin - Pdf plugin (PdfController, PdfService).
 * 
 * @author SMunikuntla
 *
 */
class PDFController {

    def generatePDF = {
		response.setContentType("application/pdf");
		response.setHeader("Content-disposition", "attachment; filename=" + (params.filename ?: "document.pdf"))
		
		// parse our markup into an xml Document
		try {
			String htmlStr = params.htmlStr
			String pathStr =  request.getScheme()+'://'+java.net.InetAddress.getLocalHost().getHostAddress() + ((request.getLocalPort() != 80) ? ':' + request.getLocalPort() : '') + request.getContextPath()
			String css = pathStr+"/css/datasetExplorer.css";
			StringBuffer buf = new StringBuffer();
			buf.append("<html><head><link rel='stylesheet' type='text/css' href='")
			.append(css).append("' media='print'/></head><body>").append(htmlStr)
			.append("</body></html>");
			
			String html = null;
			if (StringUtils.isNotEmpty(pathStr)) {
				html = StringUtils.replace(buf.toString(), '/transmart/images', pathStr+'/images')
				log.info "generatePDF replacing '"+buf.toString()+"' ==> '${html}'"
			} else {
				html = buf.toString()
			}
			
			//TODO Check if the htmlStr is a Well-Formatted XHTML string
			if (StringUtils.isNotEmpty(htmlStr)) {
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = builder.parse(new ByteArrayInputStream(html.getBytes("UTF-8")));
				ITextRenderer renderer = new ITextRenderer();
				renderer.setDocument(doc, null);
				renderer.layout();
				renderer.createPDF(response.outputStream);
				response.outputStream.flush()
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
