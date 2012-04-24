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

###########################################################################
#IC50Plot
#
###########################################################################

IC50Plot.loader <- function(
  input.filename
)
{
	#Read the IC50 data.
	doseResponse.data<-read.delim(input.filename,header=T)
	
	#If we didn't get data, alert the user.
	if(nrow(doseResponse.data) == 0) stop("||FRIENDLY|| R found no data after joining the selected concepts. Please verify that patients exist that meet your input criteria.")	
	
	#This library helps us plot a dosage response curve.
	library(drc)
	library(Cairo)
		
	#--------------
	#ALL GRAPH
	#--------------
	
	#Create the dose response curve object using the DRM function. The CELL_LINE parameter will create different curves for each group.
	drcObject <- try(drm(RESPONSE ~ DOSAGE, CELL_LINE, data = doseResponse.data,  fct = LL.4(names = c("Slope", "Lower Limit", "Upper Limit", "ED50"))), TRUE)
	
	#This initializes our image capture object.
	CairoPNG(file=paste("DOSAGE_ALL.png",sep=""), width=800, height=800,units = "px")	
	
	#Plot the Dose/Response curve.
	plot(drcObject,type="bars")
	plot(drcObject,type="all",add=TRUE)
	
	#Apply a title to the graph.
	title(paste("Dose/Response curve"))
	dev.off()
	#--------------	
	
	#--------------
	#INDIVIDUAL GRAPHS
	#--------------
	lapply(split(doseResponse.data, doseResponse.data$CELL_LINE), IC50Plot.loader.single)
	#--------------	
}

IC50Plot.loader.single <- function(
dosageData
)
{
	#Get the cell line name from the split version of the data.
	currentGroup <- unique(dosageData[['CELL_LINE']])
	currentGroup <- gsub("^\\s+|\\s+$", "",currentGroup)
	
	#Create the dose response curve object using the DRM function.
	drcObject <- try(drm(RESPONSE ~ DOSAGE, data = dosageData,  fct = LL.4(names = c("Slope", "Lower Limit", "Upper Limit", "ED50"))), TRUE)
	
	#This initializes our image capture object.
	CairoPNG(file=paste(currentGroup,"_DOSAGE.png",sep=""), width=800, height=800,units = "px")	

	#Plot the Dose/Response curve.
	plot(drcObject,type="bars")	
	plot(drcObject,type="all",add=TRUE)
	
	title(paste("Dose/Response curve for Cell Line : ",currentGroup,sep=""))
	dev.off()
}