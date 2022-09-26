package io.boomerang.config;

import java.time.OffsetDateTime;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;

/*
 * Spring Object Converter for use by MongoDB to handle:
 * Can't find a codec for class java.time.ZonedDateTime.
 * 
 * @see https://www.baeldung.com/spring-data-mongodb-zoneddatetime
 */
public class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {
    @Override
    public Date convert(OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }
}