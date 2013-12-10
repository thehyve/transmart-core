package jobs.steps

interface Step {

    /**
     * The new status name of the job just before the step gets executed, or
     * null if the job status is not to be changed.
     * @return
     */
    String getStatusName()

    void execute()

}
