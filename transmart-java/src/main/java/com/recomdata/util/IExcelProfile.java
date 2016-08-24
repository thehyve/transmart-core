package com.recomdata.util;

/**
 * Created by Florian on 05/01/14.
 */

import java.util.List;

/** Interface that is implemented by domain objects that will be exported to Excel */
public interface IExcelProfile {
    /**
     * Create the List of values that will be used to create an excel worksheet for the domain object
     *
     * @return a List (could be nested) of values
     */
    public List getValues();
}
