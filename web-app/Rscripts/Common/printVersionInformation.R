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

printVersionInformation <- 
function(outputFile)
{
	#Redirect the default output to a file.
	sink(file=outputFile, append=TRUE)
	
	#Spit the session Info out to a file.
	print(sessionInfo())
	
	print("||PACKAGEINFO||")
	
	#Spit the package info out to a file.
	packageTable <- installed.packages(fields = c ("Package", "Version"))
	
	packageTable <- data.frame(packageTable)
	
	packageTable$Depends <- gsub("\n", "",packageTable$Depends)
	packageTable$Imports <- gsub("\n", "",packageTable$Imports)
	packageTable$LinkingTo <- gsub("\n", "",packageTable$LinkingTo)
	packageTable$Suggests <- gsub("\n", "",packageTable$Suggests)
	
	write.table(packageTable,outputFile,sep="\t", append=TRUE, row.names = FALSE)

	sink()
}