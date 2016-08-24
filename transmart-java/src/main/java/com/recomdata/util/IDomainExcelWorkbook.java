package com.recomdata.util;

/**
 * Created by Florian on 05/01/14.
 */

/**
 * marker interface indicates a domain object
 * can be exposed as an Excel Workbook
 * @author jspencer
 */
public interface IDomainExcelWorkbook {

    /**
     * create an excel workbook for a domain object
     * @return
     */
    public byte[] createWorkbook();
}
