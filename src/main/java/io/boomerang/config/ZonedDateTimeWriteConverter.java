package io.boomerang.config;

import org.springframework.core.convert.converter.Converter;

import java.time.ZonedDateTime;
import java.util.Date;

/*
 * Spring Object Converter for use by MongoDB to handle:
 * Can't find a codec for class java.time.ZonedDateTime.
 * 
 * @see https://www.baeldung.com/spring-data-mongodb-zoneddatetime
 */
public class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
    @Override
    public Date convert(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }
}