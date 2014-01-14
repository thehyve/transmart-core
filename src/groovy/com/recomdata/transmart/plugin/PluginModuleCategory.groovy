package com.recomdata.transmart.plugin

/**
 * Created by Florian on 05/01/14.
 */
public enum PluginModuleCategory {
    DEFAULT("Default"),HEATMAP("Heatmap")
    final String value

    PluginModuleCategory(String value) { this.value = value }

    String toString() { value }
    String getKey() { name() }
}