###########################################################################
#BuildCorrelationDataFile
#Parse the i2b2 output file and create input files for a correlation analysis.
###########################################################################

CorrelationData.build <- 
function
(
input.dataFile,
output.dataFile="outputfile.txt",
concept.variables="",
correlation.by = ""
)
{
	#Read the input file.
	dataFile <- data.frame(read.delim(input.dataFile));
	
	#Set the column names.
	colnames(dataFile) <- c("PATIENT_NUM","SUBSET","CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH")
	
	#Split the data by the CONCEPT_PATH.
	splitData <- split(dataFile,dataFile$CONCEPT_PATH);	
	
	#Create a matrix with unique patient_nums.
	finalData <- matrix(unique(dataFile$PATIENT_NUM));	
	
	#Name the column.
	colnames(finalData) <- c("PATIENT_NUM")	
	
	#We assume we get a list of "|" separated concepts that represent the variables.
	splitConcept <- strsplit(concept.variables,"\\|");
	splitConcept <- unlist(splitConcept);

	#For each of the passed in concepts, append the rows onto the end of our temp matrix.
	for(entry in splitConcept)
	{
		#This will be a temp matrix with PATIENT_NUM,VALUE.
		tempConceptMatrix <- matrix(ncol=2,nrow=0);	
	
		tempConceptMatrix <- rbind(tempConceptMatrix,splitData[[entry]][c('PATIENT_NUM','VALUE')])		
		
		#Make the column name pretty.
		entry <- sub(pattern="^\\\\(.*?\\\\){3}",replacement="",x=entry,perl=TRUE)
		entry <- gsub("^\\s+|\\s+$", "",entry)
		entry <- gsub("^\\\\|\\\\$", "",entry)
		entry <- gsub("\\\\", "-",entry)
		
		#Add column names to our temp matrix.
		colnames(tempConceptMatrix) <- c('PATIENT_NUM',entry)
		
		#Merge the new category column into our final data matrix.
		finalData<-merge(finalData,tempConceptMatrix[c('PATIENT_NUM',entry)],by="PATIENT_NUM")		
	}			
	
	#If this is by variable, we remove the patient_num column.
	if(correlation.by == "variable")
	{
		finalData$PATIENT_NUM <- NULL
	}
	
	#We need MASS to dump the matrix to a file.
	require(MASS)
	
	#Write the final data file.
	# write.matrix(finalData,"outputfile.txt",sep = "\t")
	# Using write.table; write.matrix was leaving trailing white-space in the file - see JIRA issue TRANSREL-24.
    write.table(finalData,filename, sep = "\t", quote = FALSE, row.names = FALSE)
}
