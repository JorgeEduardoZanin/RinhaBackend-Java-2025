package jorge.rinha.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class ObjectMapperConfig implements WebFluxConfigurer {

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
  
        mapper.registerModule(new JavaTimeModule());

        mapper.registerModule(new AfterburnerModule());
        return mapper;
    }

    @Bean
    public Jackson2JsonDecoder jackson2JsonDecoder(ObjectMapper mapper) {

        return new Jackson2JsonDecoder(mapper, MimeTypeUtils.APPLICATION_JSON);
    }

    @Bean
    public Jackson2JsonEncoder jackson2JsonEncoder(ObjectMapper mapper) {
        return new Jackson2JsonEncoder(mapper, MimeTypeUtils.APPLICATION_JSON);
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {

        configurer.defaultCodecs().jackson2JsonDecoder(jackson2JsonDecoder(objectMapper()));
        configurer.defaultCodecs().jackson2JsonEncoder(jackson2JsonEncoder(objectMapper()));
    }
}
