package com.recomdata.util;

import java.util.List;

/**
 * Created by Florian on 05/01/14.
 */
public class ExcelSheet {
    private String name = null;
    private List headers = null;
    private List values = null;

    public ExcelSheet() {

    }


    public ExcelSheet(String name, List headers, List values) {
        this.name = name;
        this.headers = headers;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public List getHeaders() {
        return headers;
    }

    public List getValues() {
        return values;
    }

    public void setHeaders(List headers) {
        this.headers = headers;
    }

    public void setValues(List values) {
        this.values = values;
    }

}

