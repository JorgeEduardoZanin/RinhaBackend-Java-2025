package jorge.rinha.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

		@Bean
	    public WebClient.Builder webClientBuilder() {
	        return WebClient.builder();
	    }

		@Bean
	    public WebClient webClient(WebClient.Builder builder) {
	        return builder
	            // Garante que todo bodyValue(String) será enviado como JSON
	            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
	            .defaultHeader(HttpHeaders.ACCEPT,      MediaType.APPLICATION_JSON_VALUE)
	            // … outras configurações de pool, timeout etc.
	            .build();
	    }
	
}
