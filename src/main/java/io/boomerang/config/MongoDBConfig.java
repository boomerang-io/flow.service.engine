package io.boomerang.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/*
 * Convertor configuration for ZonedDateTime and OffsetDateTime
 * 
 * For a more advanced implementation if we want to keep the offset,
 * we may need to refer to the following two articles
 * - https://stackoverflow.com/questions/52677253/codecconfigurationexception-when-saving-zoneddatetime-to-mongodb-with-spring-boo
 * - https://jira.mongodb.org/browse/JAVA-2829
 */
@Configuration
@EnableMongoRepositories(basePackages = {"io.boomerang"})
public class MongoDBConfig extends AbstractMongoClientConfiguration {

    private final List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();

    @Override
    protected String getDatabaseName() {
        return "flow";
    }

    @Override
    public MongoCustomConversions customConversions() {
        converters.add(new ZonedDateTimeReadConverter());
        converters.add(new ZonedDateTimeWriteConverter());
        converters.add(new OffsetDateTimeReadConverter());
        converters.add(new OffsetDateTimeWriteConverter());
        return new MongoCustomConversions(converters);
    }

}