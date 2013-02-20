###########################################################################
# Copyright 2008-2012 Janssen Research & Development, LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###########################################################################


parseDirectory <- 
function
(topLevelDirectory)
{
	#We need MASS to dump the data frame to a file.
	require(MASS)

	#This is the top level directory.
	
	workingDirectory <- paste(topLevelDirectory,.Platform$file.sep,'workingDirectory',sep="")
	  
	#Get a list of all the directories underneath our temp folder.
	subDirectoryList <- dir(topLevelDirectory)

	#Remove the working directory from the list.
	subDirectoryList <- subDirectoryList[subDirectoryList != 'workingDirectory']

	#Loop through the items and see if we can find Clinical and mRNA files.
	lapply(subDirectoryList,parseSubDirectory,topLevelDirectory,workingDirectory)
}

parseSubDirectory <- function(directoryName,topLevelDirectory,workingDirectory)
{
  #This is the current directory.
  currentDirectory <- paste(topLevelDirectory,.Platform$file.sep,directoryName,.Platform$file.sep,sep="")
  
  #Create the path to the clinical/gex data file.
  clinicalDataFileLocation <- paste(currentDirectory,'Clinical',.Platform$file.sep,'clinical.i2b2trans',sep="")
  GEXDataFileLocation <- paste(currentDirectory,'mRNA',.Platform$file.sep,'Processed_Data',.Platform$file.sep,'mRNA.trans',sep="")
  
  #This is the output file.
  clinicalOutputFile <- paste(workingDirectory,.Platform$file.sep,'clinical.i2b2trans',sep='')
  GEXOutputFile <- paste(workingDirectory,.Platform$file.sep,'mRNA.trans',sep='')
  
  #Combine clinical files.
  combineParsedFiles(clinicalDataFileLocation,clinicalOutputFile,directoryName)
  
  #Combine GEX files.
  combineParsedFiles(GEXDataFileLocation,GEXOutputFile,directoryName)
}

combineParsedFiles <- function(oldFileLocation,newFileLocation,directoryName)
{
	#See if we can find the Clinical Data file.
	if(file.exists(oldFileLocation))
	{
		#Read the clinical file into memory.
		oldData <- data.frame(read.delim(oldFileLocation));

		#Add a column for our subset.
		oldData$SUBSET <- directoryName

		#Only append headers if the file doesn't exist.
		appendHeaders <- !file.exists(newFileLocation)
		
		#Write the data to a common file in the working directory.
		write.table(oldData,newFileLocation,sep = "\t",append=TRUE,col.names=appendHeaders,row.names=FALSE)

		#Delete the old file.
		unlink(oldFileLocation)
	}

}

