# WCGNA is in CRAN but depends on a package not on cran (impute),
# which is something that really should not happen on a sane repository
# Anyway, that's why it is installed here
required.packages <- c("WGCNA", "impute", "multtest", "CGHbase", "CGHtest", "CGHtestpar", "edgeR");
new.packages <- required.packages[
		!(required.packages %in% installed.packages()[,"Package"])];
if (!length(new.packages))
	q();
source("http://bioconductor.org/biocLite.R");
bioclite.packages <-
		intersect(new.packages, c("impute", "multtest", "CGHbase", "edgeR"));
if (length(bioclite.packages))
	biocLite(bioclite.packages);
if (length(intersect(new.packages, c("CGHtest")))) {
	download.file(
			url="http://files.thehyve.net/CGHtest_1.1.tar.gz",
			dest="/tmp/CGHtest_1.1.tar.gz", method="internal");
	install.packages("/tmp/CGHtest_1.1.tar.gz",
			repos=NULL, type="source")
}
if (length(intersect(new.packages, c("CGHtestpar")))) {
	download.file(
			url="http://files.thehyve.net/CGHtestpar_0.0.tar.gz",
			dest="/tmp/CGHtestpar_0.0.tar.gz", method="internal");
	install.packages("/tmp/CGHtestpar_0.0.tar.gz",
			repos=NULL, type="source")
}
if (length(intersect(new.packages, c("WGCNA")))) {
	install.packages("WGCNA", repos=Sys.getenv("CRAN_MIRROR"));
}
