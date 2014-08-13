modules = {
    // plugin needs to be declared when rmodules is used inline
    // see GPRESOURCES-176
    heatmap {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'Heatmap.js']
    }
    marker_selection {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'MarkerSelection.js']
    }
    pca {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'PCA.js']
    }
    freq_plot {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'FrequencyPlot.js']
    }
    hclust {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'HClust.js']
    }
    kclust {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'KClust.js']
    }
    acgh_survival_analysis {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'aCGHSurvivalAnalysis.js']
    }
    acgh_group_test {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'GroupTestaCGH.js']
    }
    rnaseq_group_test {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'GroupTestRNASeq.js']
    }
    scatter_plot {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'ScatterPlot.js']
    }
    forest_plot {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'ForestPlot.js']
    }
    table_fisher {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'TableWithFisher.js']
    }
    box_plot {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'BoxPlot.js']
    }
    survival_analysis {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'SurvivalAnalysis.js']
    }
    line_graph {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'LineGraph.js']
    }
    logistic_regression {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'LogisticRegression.js']
    }
    correlation_analysis {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'CorrelationAnalysis.js']
    }
    waterfall {
        resource url: [plugin: 'rdc-rmodules', dir: 'js/plugin', file: 'Waterfall.js']
    }
}

