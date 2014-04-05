package jobs

abstract class AbstractLocalRAnalysisJob extends AbstractAnalysisJob {

    File scriptsDirectory

    abstract protected List<String> getRStatements()

}
