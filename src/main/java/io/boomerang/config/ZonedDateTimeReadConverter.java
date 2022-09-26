package io.boomerang.config;

import org.springframework.core.convert.converter.Converter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/*
 * Spring Object Converter for use by MongoDB to handle:
 * Can't find a codec for class java.time.ZonedDateTime.
 * 
 * @see https://www.baeldung.com/spring-data-mongodb-zoneddatetime
 */
public class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {
    @Override
    public ZonedDateTime convert(Date date) {
        return date.toInstant().atZone(ZoneOffset.UTC);
    }
}