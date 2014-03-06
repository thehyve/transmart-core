/*************************************************************************
 * tranSMART - translational medicine data mart
 * 
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 * 
 * This product includes software developed at Janssen Research & Development, LLC.
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 ******************************************************************/
  

package com.recomdata.grails.plugin.fm

import org.transmart.biomart.Experiment
import com.recomdata.grails.plugin.fm.FmFile

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream

class FileExportController {
	
	def fmFolderService

    def add = {
		def paramMap = params
		def idList = params.id.split(',')
		
		def exportList = session['foldermanagement.exportlist']
		
		if (exportList == null) {
			exportList = [];
		}
		for (id in idList) {
			if (id && !exportList.contains(id)) {
				exportList.push(id)
			}
		}
		session['foldermanagement.exportlist'] = exportList;
		
		//Render back the number to display
		render (status: 200, text: exportList.size())
	}
	
	def remove = {
		def idList = params.list('id')
		
		def exportList = session['foldermanagement.exportlist']
		
		if (exportList == null) {
			exportList = [];
		}
		for (id in idList) {
			if (id && exportList.contains(id)) {
				exportList.remove(id)
			}
		}
		session['foldermanagement.exportlist'] = exportList;
		
		//Render back the number to display
		render (status: 200, text: exportList.size())
	}
	
	def view = {
		
		def exportList = session['foldermanagement.exportlist']
		def files = []
		for (id in exportList) {
			FmFile f = FmFile.get(id)
			if (f) {
				files.push([id: f.id, fileType: f.fileType, displayName: f.displayName, folder: fmFolderService.getPath(f.folder)])
			}
		}
		files.sort { a, b ->
			if (!a.folder.equals(b.folder)) {
				return a.folder.compareTo(b.folder)
			}
			return a.displayName.compareTo(b.displayName)
		}
		
		render(template: 'export', model: [files: files], plugin: 'folderManagement')
	}
	
	def export = {
		
		def errorResponse = []
		def filestorePath = grailsApplication.config.com.recomdata.FmFolderService.filestoreDirectory
		
		try {
			
			//Final export list comes from selected checkboxes
			def exportList = params.id.split(",")
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream()
			def zipStream = new ZipOutputStream(baos)
			
			def manifestMap = [:]
			
			for (f in exportList) {
				FmFile fmFile = FmFile.get(f)
				def fileLocation = filestorePath + "/" + fmFile.filestoreLocation + "/" + fmFile.filestoreName
				File file = new File(fileLocation)
				if (file.exists()) {

                    //Construct a file name out of the display name + suffix, if needed
                    def exportName = fmFile.displayName;
                    if (!exportName.endsWith("." + fmFile.fileType)) {
                        exportName += "." + fmFile.fileType
                    }

					String dirName = fmFolderService.getPath(fmFile.folder, true)
					if (dirName.startsWith("/") || dirName.startsWith("\\")) { dirName = dirName.substring(1) } //Lose the first separator character, this would cause a blank folder name in the zip
					def fileEntry = new ZipEntry(dirName + "/" + fmFolderService.safeFileName(exportName))
					zipStream.putNextEntry(fileEntry)
					file.withInputStream({is -> zipStream << is})
					zipStream.closeEntry()
					
					//For manifest files, add this file to a map, keyed by folder names.
					def manifestList = []
					if (manifestMap.containsKey(dirName)) {
						manifestList = manifestMap.get(dirName)
					}
					
					manifestList.push(fmFile)
					manifestMap.put(dirName, manifestList)
				}
				else {
					def errorMessage = "File not found for export: " + fileLocation
					log.error errorMessage
					errorResponse += errorMessage
				}
			}
			
			//Now for each item in the manifest map, create a manifest file and add it to the ZIP.
			def keyset = manifestMap.keySet()
			for (key in keyset) {
				def manifestEntry = new ZipEntry(key + "/" + "manifest.txt")
				zipStream.putNextEntry(manifestEntry)
				def manifestList = manifestMap.get(key)
                zipStream.write((String.format("%60s%5s%15s\n", "File Name", "Type", "Size")).getBytes())
                zipStream.write("--------------------------------------------------------------------------------\n".getBytes())
                for (fmFile in manifestList) {
                    zipStream.write((String.format("%60s%5s%15d\n", fmFile.displayName, fmFile.fileType, fmFile.fileSize)).getBytes())
                }
				zipStream.closeEntry()
			}
			
			zipStream.flush();
			zipStream.close();
			
			response.setHeader('Content-disposition', 'attachment; filename=export.zip')
			response.contentType = 'application/zip'
			response.outputStream << baos.toByteArray()
			response.outputStream.flush()
		}
		catch (Exception e) {
			log.error("Error writing ZIP", e)
			render(contentType: "text/plain", text: errorResponse.join("\n") + "\nError writing ZIP: " + e.getMessage())
		}
	}

    def exportStudyFiles = {
        def ids = []
        def folder = fmFolderService.getFolderByBioDataObject(Experiment.findByAccession(params.accession))

        def files = folder.fmFiles
        for (file in files) {
            if (file.activeInd) {
                ids.add(file.id)
            }
        }
        ids = ids.join(",")
        redirect(action: export, params: [id: ids])
    }

    def exportFile = {
        def id = params.id
        def filestorePath = grailsApplication.config.com.recomdata.FmFolderService.filestoreDirectory

        FmFile fmFile = FmFile.get(id)
        def fileLocation = filestorePath + "/" + fmFile.filestoreLocation + "/" + fmFile.filestoreName
        File file = new File(fileLocation)
        if (file.exists()) {
            String dirName = fmFolderService.getPath(fmFile.folder, true)

            //Construct a file name out of the display name + suffix, if needed
            def exportName = fmFile.displayName;
            if (!exportName.endsWith("." + fmFile.fileType)) {
                exportName += "." + fmFile.fileType
            }
            def mimeType = URLConnection.guessContentTypeFromName(file.getName())
            if (!params.open) {
                response.setHeader('Content-disposition', 'attachment; filename=' + exportName)
            }
            response.setHeader('Content-Type', mimeType)
            file.withInputStream({is -> response.outputStream << is})
            response.outputStream.flush()
        }
        else {
            render(status:500, text: "This file (" + fileLocation + ") was not found in the repository.")
        }
    }
}
