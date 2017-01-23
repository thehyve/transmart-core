
library(snpStats)
library(Cairo)

create.qq.plot <- function(inputFile)
{
	#Read the input file into a data frame.
	qqData <- read.delim(inputFile,header=T)

	#Create the image file name from the analysis ID.
	imageFileName <- paste("1",".jpg",sep="")
	
	#Create the image file.
	#Get the device ready for printing and create the image file.
	CairoPNG(file=paste("QQPlot",".png",sep=""),width=800,height=800)
	#jpeg(file=imageFileName,type='cairo',quality=90,antialias='default',res=72)
	
	#Set the graph margins.
	par(omi=c(0,0,0,0))
	
	#Create the plot.
	create.qq.plot.single(qqData)

	#Close the device to finalize the image.
	dev.off()

}

create.qq.plot.single<-function(qqData, df=2){

    #Cap the plot at 30
    x.max=30
	
	#Set the good clustering flag to 1 if the field is NA.
	qqData$good_clustering<-ifelse(is.na(qqData$good_clustering), 1, qqData$good_clustering)
	
	#Pull only the records that have a "good cluster" flag set.
	qqData<-qqData[qqData$good_clustering == 1,]
	
	#Get the study name for the label display.
	#study.name<-dbGetQuery(con, paste("SELECT set_name FROM gwa_model INNER JOIN gwa_set USING (set_id) WHERE model_id = ", model.id, sep=""))
	study.name <- "Test"
	
	#Remove the log scale from the p-value.
	#qqData$p.no.log<-10^-(qqData$pvalue)
	print(qqData)
	#Compute the chi square value.
	qqData$chi.sq<-qchisq(qqData$pvalue, df=df, log.p=F, lower.tail=F)
	
	#Generate the QQ plot.
	qq.chisq(qqData$chi.sq, df=df, slope.lambda=T, main = paste("QQ plot", study.name, sep="\n"), x.max=x.max)
}




