# TOOL acgh-plot-survival.R: "Plot survival curves for called copy number data" (Plotting of survival curves for called copy number data.)
# INPUT survival-test.txt: survival-test.txt TYPE GENE_EXPRS
# INPUT META clinical.txt: clinical.txt TYPE GENERIC 
# OUTPUT survival_*.png: survival_*.png
# PARAMETER survival: survival TYPE METACOLUMN_SEL DEFAULT survival (Phenodata column with survival data)
# PARAMETER status: status TYPE METACOLUMN_SEL DEFAULT status (Phenodata column with patient status: alive=0, dead=1)
# PARAMETER aberrations: aberrations TYPE [gains: gains, losses: losses, both: both] DEFAULT both (Whether to test only for gains or losses, or both.) 
# PARAMETER confidence.intervals: "plot confidence intervals" TYPE [yes: yes, no: no] DEFAULT no (Whether to plot the confidence intervals.) 

# Ilari Scheinin <firstname.lastname@gmail.com>, adapted by Tim Hulsen for use in tranSMART
# 2013-05-08

acgh.plot.survival <- function
(
  survival='Overall survival time',
  status='Survival status',
  aberrations='both',
  confidence.intervals='no'
)
{

  library(survival)

  dat <- read.table('survival-test.txt', header=TRUE, sep='\t', quote='', row.names=1, as.is=TRUE, check.names=FALSE)
  phenodata <- read.table('clinical.txt', header=TRUE, sep='\t', check.names=FALSE)

  phenodata_<-reshape(phenodata,v.names=c("CONCEPT_CODE","CONCEPT_PATH_SHORT","VALUE","CONCEPT_PATH"),timevar="CONCEPT_PATH_SHORT",idvar="PATIENT_NUM",direction='wide')
  colnames(phenodata_)<-c("PATIENT_NUM","SUBSET","CONCEPT_CODE_OST","CONCEPT_PATH_SHORT_OST","Overall survival time","CONCEPT_PATH_OST","CONCEPT_CODE_SS","CONCEPT_PATH_SHORT_SS","Survival status","CONCEPT_PATH_SS")
  phenodata_[,"Overall survival time"]<-as.numeric(as.character(phenodata_[,"Overall survival time"]))
  phenodata_[,"Survival status"]<-as.character(phenodata_[,"Survival status"])
  phenodata_[,"Survival status"][is.na(phenodata_[,"Survival status"])] <- 0
  phenodata_[,"Survival status"][phenodata_[,"Survival status"]!=0] <- 1
  phenodata_[,"Survival status"]=as.numeric(phenodata_[,"Survival status"])
  
  s <- Surv(phenodata_[,survival], phenodata_[,status])
  reg <- as.matrix(dat[, grep('^flag\\.', colnames(dat))])
  if (aberrations == 'gains') {
    reg[reg > 0] <- 1
    reg[reg < 0] <- 0
    call.legend <- c('loss', 'no-gain', 'gain')
  } else if (aberrations == 'losses') {
    reg[reg > 0] <- 0
    reg[reg < 0] <- -1
    call.legend <- c('loss', 'no-loss', 'gain')
  } else {
    call.legend <- c('loss', 'normal', 'gain')
  }
  names(call.legend) <- -1:1
  call.cols <- c('red', 'black', 'blue')
  names(call.cols) <- -1:1
  for (i in rownames(dat)) {
    f <- survfit(s ~ reg[i,])
    call <- sub('^.*=(.*)', '\\1', names(f$strata))
    n <- length(call)
    legend <- paste(call.legend[call], ' (', f$n, ')', sep='')
    cols <- call.cols[call]
    ltys <- rep(0, n)
    pchs <- rep(22, n)
    if (!is.null(dat$cytoband)) {
      main <- paste('Survival for', dat[i, 'cytoband'])
    } else {
      main <- paste('Survival for chr.',dat[i, 'chromosome'],', ',dat[i, 'start'],'-',dat[i, 'end'],', ',aberrations)
    }
    pngname<-paste('survival_',dat[i, 'chromosome'],'_',dat[i, 'start'],'_',dat[i, 'end'],'_',aberrations,'.png',sep='')
    png(pngname)
    plot(f, main=main, xlab='t', ylab=expression(hat(S)(t)), col=cols)
    if (confidence.intervals == 'yes') {
      lines(f, conf.int='only', lty=2, col=cols)
      legend <- c(legend, 'survival', '95% confidence interval')
      cols <- c(cols, 'black', 'black')
      ltys <- c(ltys, 1, 2)
      pchs <- c(pchs, NA, NA)
    }
    legend('bottomleft', legend=legend, lty=ltys, pch=pchs, pt.bg=cols, pt.cex=2, inset=0.01)
    mtext(paste(dat[i, 'chromosome'], ':', format(dat[i, 'start'], big.mark=','), '-', format(dat[i, 'end'], big.mark=','), sep=''), side=1, line=-1, adj=0.99)
    if (!is.null(dat$fdr))
      mtext(paste('FDR = ', sprintf('%.4f', dat[i, 'fdr'])), side=1, line=-2, adj=0.99)
    if (!is.null(dat$pvalue))
      mtext(paste('p = ', sprintf('%.4f', dat[i, 'pvalue'])), side=1, line=-3, adj=0.99)
    dev.off()
  }

}

# EOF
