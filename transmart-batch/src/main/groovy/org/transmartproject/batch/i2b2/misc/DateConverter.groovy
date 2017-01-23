package org.transmartproject.batch.i2b2.misc

import groovy.transform.CompileStatic
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Date conversions.
 */
@Component
@JobScope
@CompileStatic
class DateConverter implements Converter<String, Date> {

    @Value("#{jobParameters['DATE_FORMAT']}")
    String dateFormat // not mandatory

    private SimpleDateFormat simpleDateFormat // if dateFormat is given

    @PostConstruct
    void init() {
        if (dateFormat != null) {
            simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.ENGLISH)
        }
    }

    boolean isCustomFormat() {
        dateFormat != null
    }

    Date parse(String dateString) throws IllegalArgumentException {
        if (simpleDateFormat) {
            synchronized (simpleDateFormat) {
                try {
                    simpleDateFormat.parse(dateString)
                } catch (ParseException pe) {
                    throw new IllegalArgumentException(pe)
                }
            }
        } else {
            // assume ISO8601
            DatatypeConverter.parseDateTime(dateString).time
        }
    }

    @Override
    Date convert(String source) {
        parse source
    }
}
