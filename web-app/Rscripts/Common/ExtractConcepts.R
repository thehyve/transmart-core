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
#ExtractConcepts
#Take a data frame that has been split and create a matrix from a list of concepts that need to be extracted from that matrix.
###########################################################################

#This function will take the split concept file and extract the concepts we are interested in. Returns a matrix of PATIENT_NUM,VALUE.
extractConcepts <- function
(
	splitData,
	conceptList,
	fullConcept = FALSE,
	conceptColumn = FALSE,
	encounterColumn = FALSE
)
{

	print("         -------------------")
	print("         ExtractConcepts.R")
	print("         Extracting DATA")
	print(paste("         Concept List - ", conceptList, sep=""))

	##########################################
	#Get a table of Concept value/Patient
	splitConcept <- strsplit(conceptList,"\\|");
	splitConcept <- unlist(splitConcept);

	#This will be a temp matrix with VALUE.
	tempConceptMatrix <- matrix(ncol=1,nrow=0);
	
	#These are the final column names we return.
	finalColumnNames <- c('PATIENT_NUM','VALUE')
	
	#Start a list of columns to pull from the temporary matrix.
	columnListToPull <- c('PATIENT_NUM')

		#If we are doing manual categorical binning, we need to keep the concept path, otherwise we can just use the short value.
	if(fullConcept && conceptColumn)		
		{
		columnListToPull <- c(columnListToPull,'CONCEPT_PATH', 'CONCEPT_PATH')
		finalColumnNames <- c('PATIENT_NUM','CONCEPT_PATH', 'VALUE')
				
		}
	else if(!fullConcept && conceptColumn)
		{
		columnListToPull <- c(columnListToPull,'CONCEPT_PATH', 'VALUE')
			finalColumnNames <- c('PATIENT_NUM','CONCEPT_PATH','VALUE')
		}
	else if(fullConcept && !conceptColumn)
	{
		columnListToPull <- c(columnListToPull, 'CONCEPT_PATH')
		finalColumnNames <- c('PATIENT_NUM', 'VALUE')	
	}
		else
		{
		columnListToPull <- c(columnListToPull, 'VALUE')
	}
	if(encounterColumn)
	{
		columnListToPull <- c(columnListToPull, 'ENCOUNTER_NUM', 'LINK_TYPE')
		finalColumnNames <- c(finalColumnNames, 'ENCOUNTER_NUM', 'LINK_TYPE')		
		}
	#For each of the passed in concepts, append the rows onto the end of our temp matrix.
	for(entry in splitConcept)
	{
		tempConceptMatrix <- rbind(tempConceptMatrix,splitData[[entry]][columnListToPull])
	}

	#Make sure we actually have data after the binding.
	if(length(tempConceptMatrix)==0) stop("||FRIENDLY||R found no data after joining the selected concepts. Please verify that patients exist that meet your input criteria.")
	
	#Add column names to our temp matrix.
	colnames(tempConceptMatrix) <- finalColumnNames
	
	print("         -------------------")
	
	return(tempConceptMatrix)
	##########################################
}
