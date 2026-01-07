package de.oth.kleinanzeigen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Arrays;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(Arrays.asList(new LongToInstantConverter()));
    }

    private static class LongToInstantConverter implements Converter<Long, Instant> {
        @Override
        public Instant convert(@NonNull Long source) {
            return Instant.ofEpochMilli(source);
        }
    }
}
