#!/usr/bin/env groovy
if (args.length < 2) {
    println "Usage: runJob.groovy <jobPath> <jobId> [jobArgs...]"
    System.exit(1)
}

jobPath = args[0]
jobId = args[1]
//no way (yet) on the gradle task side of receiving the optional project-prop 'jobArgs', so not passed yet
//jobArgs =

def proc = "gradle -PjobPath=$jobPath -PjobId=$jobId runJob".execute()
proc.consumeProcessOutput(System.out, System.err)
