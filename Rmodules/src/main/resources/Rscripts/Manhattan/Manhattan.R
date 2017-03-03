args = commandArgs(trailingOnly=TRUE)
library(Cairo)

threshold<-5e-8
maximumPeaks<-1
chr_X<-"23"
chr_Y<-"24"
chr_M<-"25"
chr_X_str<-"X"
chr_Y_str<-"Y"
chr_M_str<-"M"
points_shape<-19
annotationSize<-1
imgWidth<-1000
imgHeight<-800
peaks<-NULL

create.manhattan.plot <- function(inputFile=inputFile,studyFileName=NULL, studyName=NULL) {
  png(file=paste("manhattan",".png",sep=""),width=imgWidth,height=imgHeight, type="cairo")
  res<-read.table(inputFile, header=TRUE, sep="\t")
 
  plotTitle <- "Manhattan Plot"

  if (!is.null(studyFileName)){
	  plotTitle <- paste(as.character(read.table(studyFileName, sep="\t", header=TRUE)$STUDY_NAME[1]),"-", plotTitle, sep =" ") 
  }
  if (!is.null(studyName)){
    plotTitle <- paste(studyName,"-", plotTitle, sep =" ") 
  }

  res<-res[which(!is.na(res$chrom)),] # remove data where chrom is null
  
  pvalue<-as.vector(res$pvalue)
  chr <-as.vector(res$chrom)
  pos<-as.vector(res$pos)
  annots<-as.vector(res$gene)
  
  allChr <- unique(res$chrom)
  
  # get one peak per chromosome
  for(c in allChr) {
    
    c_res<-res[which(res$pvalue<threshold),]
    c_res<-c_res[which(c_res$chrom == c),]
    c_res<-c_res[order(c_res$pvalue),]
    
    if (dim(c_res)[1] > maximumPeaks) {
      c_res<-c_res[1:maximumPeaks,]
    }
    
    if (is.null(peaks)) {
      peaks = c_res
    } 
    else {
      peaks<-rbind(peaks,c_res)
    }
  
      
  }
  
  # for computing purpose replace X,Y,M chrom with numbers
  if (length(which(chr==chr_X_str)) > 0) { 
    chr[which(chr==chr_X_str)]<-chr_X
  }
  if (length(which(chr==chr_Y_str)) > 0) {
    chr[which(chr==chr_Y_str)]<-chr_Y
  }
  if (length(which(chr==chr_M_str)) > 0) {
    chr[which(chr==chr_M_str)]<-chr_M
  }
  
  res$chrom<-as.numeric(chr)
  res<-res[order(as.numeric(res$chrom), res$pos),]
  
  MakingManhattan23andMeStyleFanciestAlt3(res$pvalue, res$chrom, res$pos, res$gene, maintext=plotTitle, peaks=peaks)
  dev.off()
}

MakingManhattan23andMeStyleFanciestAlt3 <- function(pee,chr,pos,annots,backannots=NULL,upper.limit=NULL,peak.flag=NULL,maintext=NULL,shiftups=NULL,shiftdowns=NULL, peaks=NULL) {
  
  annots <- as.character(annots);
  chr.limits <- aggregate(pos,by=list(chr),FUN=max,na.rm=T)
  numchr <- dim(chr.limits)[1];
  row.names(chr.limits) <- chr.limits$Group.1;
  
  evenses <- seq(2,numchr,2);
  odds <- seq(1,numchr,2);
  neven <- length(evenses);
  evencols <- rainbow(neven);
  revenses <- c(seq(ceiling(neven/2),neven,1),seq(1,ceiling(neven/2)-1,1));
  
  nodd <- length(odds);
  oddcols <- rainbow(nodd);
  
  thosecols <- rep(NA,times=numchr);
  
  thosecols[evenses] <- "#6bcedd"; #"#b0e0b0";
  thosecols[odds] <- "#3955a6"; #"#b0b0e0";
 
  uppercol <- "red";
  
  inap <- is.na(pee) | !is.finite(pee);
  upperbound <- ceiling(0.25 + min(upper.limit,max(-log10(pee[!inap]))+2) );

  if (upperbound < 16) {
	  upperbound <- 16;
  }
  
  peaksChr<-as.integer(as.vector(peaks$chrom))
  peaksPos<-as.vector(peaks$pos)
  chrAxisLabel<-as.character(seq(1,numchr,1))
  
  chrAxisLabel[which(chrAxisLabel==chr_X)]<-chr_X_str
  chrAxisLabel[which(chrAxisLabel==chr_Y)]<-chr_Y_str
  chrAxisLabel[which(chrAxisLabel==chr_M)]<-chr_M_str
  
  plot(chr+pos/(1+chr.limits[as.character(chr),"x"]),c(0,rep(upperbound,times=length(pos)-1)),xlab="Chromosome",ylab="-log10(pval)",main=maintext,yaxp=c(0,upperbound,upperbound),type="n",xaxt="n",bty="l", ylim=c(0,upperbound))
  axis(side=1,at=seq(1,numchr,1)+0.5,labels=chrAxisLabel);
  points(chr+pos/(1+chr.limits[as.character(chr),"x"]),-log10(pee),col=thosecols[chr],lwd=2, pch=points_shape);
  
  limt <- -log10(threshold)
  
  iio <- -log10(pee) > limt
  
  points((chr+pos/(1+chr.limits[as.character(chr),"x"]))[iio],-log10(pee)[iio],col=thosecols[chr][iio],lwd=2, pch=points_shape)
  if (length(peaksPos) > 0) {
    text(peaksChr+peaksPos/(1+chr.limits[as.character(peaksChr),"x"]),-log10(peaks$pvalue)+(upperbound/40),labels=peaks$gene, cex=annotationSize, adj=0)
  }
  
  abline(h=-log10(threshold),col="red")
  
  if (!is.null(peak.flag)) {
    
    peaks <- data.frame(cbind(pee=as.numeric(pee[peak.flag]),chr=chr[peak.flag],pos=pos[peak.flag]));
    peaks$annots <- as.character(annots)[peak.flag];
    #otherwise co-erce number types to factors or chars
    
    if (!is.null(backannots)) {
      peaks$backannots <- as.character(backannots)[peak.flag];
    }
    
    peaks$over <- (peaks$pee < 10^(-1*upper.limit))
    
    
    shifts <- rep(0,times=length(peaks$pee));
    
    if(!is.null(shiftups)) {
      shifts[shiftups] <- 0.5;
    }
    
    if(!is.null(shiftdowns)) {
      shifts[shiftdowns] <- -0.5;
    }	
    
    bigverts2 <- ifelse(-log10(peaks$pee) > upper.limit,upper.limit+0.5,-log10(peaks$pee)+ 0.5 + shifts);
    
    io <- peaks$over;
    iunder <- peaks$pee < 5.0e-08
    coltxt <- rep("black",times=dim(peaks)[1])
    coltxt[io] <- "blue";
    coltxt[!iunder] <- "black"
    
    iempty <- peaks$annots == "[]";
    if (!is.null(backannots)) {
      peaks$annots[iempty] <- peaks$backannots[iempty];
    }
    
    if (dim(peaks)[1]>0){    
    	text(peaks$chr+peaks$pos/(1+chr.limits[as.character(peaks$chr),"x"]),bigverts2,peaks$annots,col=coltxt,font=2,cex=1.2);
    }
    
    
    if(sum(io) > 0) {
      text(peaks$chr[io]+peaks$pos[io]/(1+chr.limits[as.character(peaks$chr[io]),"x"]),bigverts2[io]+1,paste("peak p:",format(peaks$pee[io],digits=2),sep=""),col="blue");
    }
  }
  
 
}

