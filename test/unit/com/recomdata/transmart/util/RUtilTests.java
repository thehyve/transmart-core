package com.recomdata.transmart.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class RUtilTests {

    @Test
    public void basicTest() {
        String t = "alea iacta est\u5050\"' \n\t\\\u0007\b\f\r\u000b";

        String escaped = RUtil.escapeRStringContent(t);
        assertEquals("alea iacta est\\u5050\\\"\\' \\n\\t\\\\" +
                "\\a\\b\\f\\r\\v", escaped);
    }

}
