package jorge.rinha.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import jorge.rinha.dto.response.HealthResponse;

@RestController
@RequestMapping("/teste")
public class teste {

	private final WebClient webClientFallbackHealth;

	public teste(WebClient.Builder webClientBuilder, @Value("${url.health.fallback}") String urlFallbackHealth) {
		this.webClientFallbackHealth = webClientBuilder.baseUrl(urlFallbackHealth).build();
	}

	@GetMapping
	public ResponseEntity<HealthResponse> testeaa() {
		var tt = webClientFallbackHealth.get()
			.retrieve()
			.bodyToMono(HealthResponse.class)
			.block();

		return ResponseEntity.ok(tt);
	}
}
