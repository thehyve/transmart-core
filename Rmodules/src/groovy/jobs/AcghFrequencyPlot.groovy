package jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class AcghFrequencyPlot extends AcghAnalysisJob {

    @Override
    protected List<String> getRStatements() {
        [
                '''source('$pluginDirectory/aCGH/acgh-frequency-plot.R')''',
                '''acgh.frequency.plot(column = 'group')'''
        ]
    }

    @Override
    protected getForwardPath() {
        return "/AcghFrequencyPlot/acghFrequencyPlotOutput?jobName=${name}"
    }
}
