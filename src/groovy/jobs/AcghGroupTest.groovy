package jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class AcghGroupTest extends AcghAnalysisJob {

    @Override
    protected List<String> getRStatements() {
        [
                '''source('$pluginDirectory/aCGH/acgh-group-test.R')''',
                '''acgh.group.test(column = 'group',
                                   test.statistic = '$statisticsType',
                                   test.aberrations = '$aberrationType')'''
        ]
    }

    @Override
    protected getForwardPath() {
        return "/aCGHgroupTest/aCGHgroupTestOutput?jobName=${name}"
    }
}
