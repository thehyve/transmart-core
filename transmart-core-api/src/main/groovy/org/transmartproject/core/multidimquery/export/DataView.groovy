package org.transmartproject.core.multidimquery.export

import com.fasterxml.jackson.annotation.JsonCreator

enum DataView {

    DATA_TABLE('dataTable'),
    SURVEY_TABLE('surveyTable'),
    NONE('none')

    private String view

    DataView(String view) {
        this.view = view
    }

    private static final Map<String, DataView> mapping = new HashMap<>()
    static {
        for (DataView dataView: values()) {
            mapping.put(dataView.view.toLowerCase(), dataView)
        }
    }

    @JsonCreator
    static DataView from(String view) {
        view = view.toLowerCase()
        if (mapping.containsKey(view)) {
            return mapping.get(view)
        } else {
            return NONE
        }
    }

    String toString() {
        view
    }

}
