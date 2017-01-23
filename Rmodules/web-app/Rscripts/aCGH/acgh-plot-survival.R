# TOOL acgh-plot-survival.R: "Plot survival curves for called copy number data" (Plotting of survival curves for called copy number data.)
# INPUT survival-test.txt: survival-test.txt TYPE GENE_EXPRS
# INPUT META phenodata.tsv: phenodata.tsv TYPE GENERIC 
# OUTPUT survival_*.png: survival_*.png
# PARAMETER survival: survival TYPE METACOLUMN_SEL DEFAULT survival (Phenodata column with survival data)
# PARAMETER status: status TYPE METACOLUMN_SEL DEFAULT status (Phenodata column with patient status: alive=0, dead=1)
# PARAMETER censor: censor TYPE METACOLUMN_SEL DEFAULT <zero length string> (Phenodata column with censor status: censored=1, not censored=0)
#                   If non-zero length censor parameter is specified, the censor status column in Phenodata takes precedence over the status column.
# PARAMETER aberrations: aberrations TYPE [gains: gains, losses: losses, both: both] DEFAULT both (Whether to test only for gains or losses, or both.) 
# PARAMETER confidence.intervals: "plot confidence intervals" TYPE [yes: yes, no: no] DEFAULT no (Whether to plot the confidence intervals.) 

# Ilari Scheinin <firstname.lastname@gmail.com>, adapted by Tim Hulsen for use in tranSMART
# 2013-05-08

acgh.plot.survival <- function
(
  survival='Overall survival time',
  status='Survival status',
  censor='',
  aberrations='both',
  confidence.intervals='no'
)
{
  library(survival)
  library(foreach)
  library(doParallel)
  library(Cairo)

  nrcpus=0
  try({
    nrcpus=as.numeric(system("nproc", intern=TRUE))
  }, silent=TRUE);
  if(nrcpus<1) {
    nrcpus=1
  }
  registerDoParallel(nrcpus)

  dat <- read.table('survival-test.txt', header=TRUE, sep='\t', quote='', row.names = NULL, as.is=TRUE, check.names=FALSE)
  # To enforce preservation of rownames by as.matrix, the automatic generated rownames need to be made explicit
  rownames(dat) <- c(1:nrow(dat))

  phenodata <- read.table('phenodata.tsv', header=TRUE, sep='\t', check.names=FALSE)

  # Check if non-zero length censoring status parameter has been provided
  if ( nchar(censor) > 0 )
  {  # Use censor column to calculate patient status column
     #   censor.1 (censored_yes) -> status.0 (alive)
     #   censor.0 (censored_no ) -> status.1 (dead)
     phenodata[,status] <- as.integer(!phenodata[,censor])
  }

  # Extract sample list from aCGH data column names for which calls (flag) have been observed
	samplelist <- sub("flag.", "" , colnames(dat)[grep('flag.', colnames(dat))] )
	# Make row names equal to the sample id
	rownames(phenodata) <- phenodata[,"PATIENT_NUM"]
	# Reorder phenodata rows to match the order in the aCGH data columns
	phenodata <- phenodata[samplelist,,drop=FALSE]
	
  # Allow both numeric as well as string argument for aberrations parameter (acgh.survival.test function expects numeric argument values)
  aberrations_dic <- c('losses','losses','both','both','gains','gains')
  names(aberrations_dic) <- c('-1','losses','0','both','1','gains')
  aberrations<-toString(aberrations)
  if ( !(aberrations %in% names(aberrations_dic)) )
  {
    aberrations <- 'both'
  } else {
    aberrations <- aberrations_dic[[aberrations]]
  }

  s <- Surv(phenodata[,survival], phenodata[,status])
  reg <- as.matrix(dat[, grep('^flag\\.', colnames(dat))])
  # we map the the values -2 and 2 on -1 and 1 respectively
  reg[reg[] ==  2] <-  1
  reg[reg[] == -2] <- -1
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
  foreach (i=rownames(dat)) %dopar% {
    f <- survfit(s ~ reg[i,])
    call <- sub('^.*=(.*)', '\\1', names(f$strata))
    n <- length(call)
    legend <- paste(call.legend[call], ' (', f$n, ')', sep='')
    cols <- call.cols[call]
    ltys <- rep(0, n)
    pchs <- rep(22, n)
    if (is.null(dat$cytoband) | is.na(dat[i, 'cytoband']) | dat[i, 'cytoband'] == '') {
      main <- paste('Survival for chr.',dat[i, 'chromosome'],', ',dat[i, 'start'],'-',dat[i, 'end'],', ',aberrations)
    } else {
      main <- paste('Survival for', dat[i, 'cytoband'])
    }
    pngname<-paste('aCGHSurvivalAnalysis_',dat[i, 'chromosome'],'_',dat[i, 'start'],'_',dat[i, 'end'],'.png',sep='')
    CairoPNG(file=pngname)
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
