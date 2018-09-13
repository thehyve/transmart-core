package org.transmartproject.rest.utils

import java.text.DateFormat
import java.text.SimpleDateFormat

class DateUtils {
    static String formatAsISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone 'UTC'
        DateFormat df = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss\'Z\'')
        df.timeZone = tz
        df.format date
    }
}
