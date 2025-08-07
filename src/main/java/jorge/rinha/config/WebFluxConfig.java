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
public class WebFluxConfig implements WebFluxConfigurer {

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        // módulo para suportar JavaTime (Instant, etc)
        mapper.registerModule(new JavaTimeModule());
        // Afterburner para acelerar serialização
        mapper.registerModule(new AfterburnerModule());
        return mapper;
    }

    @Bean
    public Jackson2JsonDecoder jackson2JsonDecoder(ObjectMapper mapper) {
        // passa o ObjectMapper + só JSON
        return new Jackson2JsonDecoder(mapper, MimeTypeUtils.APPLICATION_JSON);
    }

    @Bean
    public Jackson2JsonEncoder jackson2JsonEncoder(ObjectMapper mapper) {
        return new Jackson2JsonEncoder(mapper, MimeTypeUtils.APPLICATION_JSON);
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // sobrescreve os codecs padrão de JSON
        configurer.defaultCodecs().jackson2JsonDecoder(jackson2JsonDecoder(objectMapper()));
        configurer.defaultCodecs().jackson2JsonEncoder(jackson2JsonEncoder(objectMapper()));
    }
}
