# TOOL acgh-group-test.R: "Group tests for called copy number data" (Statistical tests between two or more groups for called copy number data. The testing is recommended to be performed after running the Identify common regions from called copy number data tool.)
# INPUT regions.tsv: regions.tsv TYPE GENE_EXPRS 
# INPUT META phenodata.tsv: phenodata.tsv TYPE GENERIC 
# OUTPUT groups-test.txt: groups-test.txt 
# OUTPUT groups-test.png: groups-test.png
# PARAMETER column: column TYPE METACOLUMN_SEL DEFAULT group (Phenodata column describing the groups to test)
# PARAMETER test.statistic: test.statistic TYPE [Chi-square: Chi-square, Wilcoxon: Wilcoxon, KW: KW] DEFAULT Chi-square (The test to use: either Chi-square, Wilcoxon, or Kruskal-Wallis.)
# PARAMETER number.of.permutations: number.of.permutations TYPE INTEGER DEFAULT 10000 (The number of permutations. At least 10000 recommended for final calculations.)
# PARAMETER test.aberrations: test.aberrations TYPE [1: gains, -1: losses, 0: both] DEFAULT 0 (Whether to test only for gains or losses, or both.) 

# Ilari Scheinin <firstname.lastname@gmail.com>, adapted by Tim Hulsen for use in tranSMART
# 2013-05-08

acgh.group.test <- function
(
  column='group',
  test.statistic='Chi-square',
  number.of.permutations=10000,
  test.aberrations=0
)
{
  library(Cairo)

  aberrations_dict <- c('loss', 'both', 'gain')
  aberrations_options <- c('-1', '0', '1')
  names(aberrations_dict) <- aberrations_options
  
	aberrations <- aberrations_dict[['0']]
	test.aberrations <- as.character(as.integer(test.aberrations))
	if (test.aberrations %in% aberrations_options)
	{
	  aberrations <- aberrations_dict[[test.aberrations]]
	}

  dat       <- read.table('outputfile.txt', header=TRUE, sep='\t', quote='"', as.is=TRUE,       check.names=FALSE, stringsAsFactors = FALSE)
  phenodata <- read.table('phenodata.tsv',  header=TRUE, sep='\t', quote='"', strip.white=TRUE, check.names=FALSE, stringsAsFactors = FALSE)

  groupnames <- unique(phenodata[,column])
  groupnames <- groupnames[!is.na(groupnames)]
  groupnames <- groupnames[groupnames!='']

  first.data.col <- min(grep('chip', names(dat)), grep('flag', names(dat)))
  data.info <- dat[,1:(first.data.col-1)]

  calls <- as.matrix(dat[,grep('flag', colnames(dat))])

  datacgh <- data.frame()
  group.sizes <- integer()
  for (group in groupnames) {
    # Get ids for patients in group
    group.samples <- which(phenodata[,column] == group & !is.na(phenodata[,column]))
    group.ids     <- phenodata[group.samples, "PATIENT_NUM"]

    # Match data with selected patients
    highdimColumnsMatchingGroupIds <- match(paste("flag.",group.ids,sep=""), colnames(calls))
    highdimColumnsMatchingGroupIds <- highdimColumnsMatchingGroupIds[which(!is.na(highdimColumnsMatchingGroupIds))]
    group.calls   <- calls[ , highdimColumnsMatchingGroupIds, drop=FALSE]
    if (nrow(datacgh)==0) {
      datacgh <- group.calls
    } else {
      datacgh <- cbind(datacgh, group.calls)
    }
    group.sizes <- c(group.sizes, length(highdimColumnsMatchingGroupIds))
    data.info[,paste('loss.freq.', group, sep='')] <- round(rowMeans(group.calls == -1), digits=3)
    data.info[,paste('gain.freq.', group, sep='')] <- round(rowMeans(group.calls == 1), digits=3)
    if (2 %in% calls)
      data.info[,paste('amp.freq.', group, sep='')] <- round(rowMeans(group.calls == 2), digits=3)
  }

  nrcpus=0
  try ({
    nrcpus=as.numeric(system("nproc", intern=TRUE))
  }, silent=TRUE);
  if(nrcpus<1) {
    nrcpus=2
  }
  # first try parallel computing
  prob <- TRUE
  try({
    library(CGHtestpar)
    pvs <-  pvalstest(datacgh, data.info, teststat=test.statistic, group=group.sizes, groupnames=groupnames, lgonly=as.integer(test.aberrations), niter=number.of.permutations, ncpus=nrcpus)
    fdrs <- fdrperm(pvs)
    prob <- FALSE
  }, silent=TRUE)
  # if problems, fall back to sequential computing
  if (prob) {
    library(CGHtest)
    pvs <-  pvalstest(datacgh, data.info, teststat=test.statistic, group=group.sizes, groupnames=groupnames, lgonly=as.integer(test.aberrations), niter=number.of.permutations)
    fdrs <- fdrperm(pvs)
  }

  # Replace chromosome X with number 23 to get only integer column values
  fdrs$chromosome[fdrs$chromosome=='X']         <- 23
  fdrs$chromosome[fdrs$chromosome=='XY']        <- 24
  fdrs$chromosome[fdrs$chromosome=='Y']         <- 25
  fdrs$chromosome[fdrs$chromosome=='M']         <- 26
  fdrs$chromosome[fdrs$chromosome=='[:alpha:]'] <- 0
  fdrs$chromosome[fdrs$chromosome=='']          <- 0
  fdrs$chromosome <- as.integer(fdrs$chromosome)
  # Order by chromosome and start bp to ensure correct chromosome labels in frequency plots
  fdrs <- fdrs[with(fdrs,order(chromosome,start)),]
  
  options(scipen=10)
  #filename <- paste('groups-test-',aberrations,'.txt',sep='')
  filename <- paste('groups-test','.txt',sep='')
  write.table(fdrs, file=filename, quote=FALSE, sep='\t', row.names=FALSE, col.names=TRUE)

  FDRplot <- function(fdrs, which, main = 'Frequency Plot with FDR',...) {
    par(mar=c(5,4,4,5) + 0.1)
    cols <- c('blue', 'red')
    names(cols) <- c('gain', 'loss')

    chromosomes <- fdrs$chromosome
    a.freq <- fdrs[,paste(which, '.freq.', groupnames[1], sep='')]
    b.freq <- fdrs[,paste(which, '.freq.', groupnames[2], sep='')]
    fdr <- fdrs$fdr
    
    if ('num.probes' %in% colnames(fdrs) & !any(is.na(fdrs$num.probes))) {
      chromosomes <- rep(chromosomes, fdrs$num.probes)
      a.freq <- rep(a.freq, fdrs$num.probes)
      b.freq <- rep(b.freq, fdrs$num.probes)
      fdr <- rep(fdr, fdrs$num.probes)
    }

    plot(a.freq, ylim=c(-1,1), type='h', col=cols[which], xlab='chromosomes', ylab='frequency', xaxt='n', yaxt='n', main=main, ...)
    points(-b.freq, type='h', col=cols[which])
    abline(h=0)
    abline(v=0, lty='dashed')
    cs.chr = cumsum(table(chromosomes))
    for(i in cs.chr)
      abline(v=i, lty='dashed')
      
    ax <- (cs.chr + c(0,cs.chr[-length(cs.chr)])) / 2
    lbl.chr <- unique(chromosomes)
    lbl.chr[lbl.chr==0]  <- 'U'
    lbl.chr[lbl.chr==23] <- 'X'
    lbl.chr[lbl.chr==24] <- 'XY'
    lbl.chr[lbl.chr==25] <- 'Y'
    lbl.chr[lbl.chr==26] <- 'M'
    
    axis(side=1, at=ax, labels=lbl.chr, las=2)
    axis(side=2, at=c(-1, -0.5, 0, 0.5, 1), labels=c('100 %', ' 50 %', '0 %', '50 %', '100 %'), las=1)
    logfdr <- -log10(fdr)
    logfdr[logfdr == Inf] <- 10
    points(logfdr - 1, type='l')
    labels <- c(0.01, 0.05, 0.025, 0.1, 0.25, 0.5, 1)
    axis(side=4, at=-log10(labels) - 1, labels=labels, las=1)
    mtext('FDR', side=4, line=3)
    mtext(groupnames[1], side=2, line=3, at=0.5)
    mtext(groupnames[2], side=2, line=3, at=-0.5)
  }

  # Not necessary to use aberration specific file name for image file
  # Remove aberrations from image file name
  # filename <- paste('groups-test-',aberrations,'.png',sep='')
  filename <- paste('groups-test','.png',sep='')
  
  if (aberrations == 'both')
  {
    CairoPNG(file=filename, width=1000, height=800)
    par(mfrow = c(2,1))
  } else {
    CairoPNG(file=filename, width=1000, height=400)
  }

  if (aberrations != 'loss')
  {
    FDRplot(fdrs, 'gain', 'Frequency Plot of Gains with FDR')
  }
  if (aberrations !=  'gain')
  {
    FDRplot(fdrs, 'loss', 'Frequency Plot of Losses with FDR')
  }
  dev.off()
}

# EOF
