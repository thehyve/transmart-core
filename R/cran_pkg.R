required.packages <- c("reshape2", "ggplot2", "data.table", "Cairo",
		"snowfall", "gplots", "Rserve", "foreach", "doParallel", "visreg");
missing.packages <- function(required) {
	return(required[
		!(required %in% installed.packages()[,"Package"])]);
}
new.packages <- missing.packages(required.packages)
if (length(new.packages)) {
	install.packages(new.packages, repos=Sys.getenv("CRAN_MIRROR"));
}

if (length(missing.packages(required.packages))) {
	warning('Some packages not installed');
	quit(1);
}
