package io.boomerang.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;

/*
 * Spring Object Converter for use by MongoDB to handle:
 * Can't find a codec for class java.time.ZonedDateTime.
 * 
 * @see https://www.baeldung.com/spring-data-mongodb-zoneddatetime
 */
public class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {
    @Override
    public OffsetDateTime convert(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }
}