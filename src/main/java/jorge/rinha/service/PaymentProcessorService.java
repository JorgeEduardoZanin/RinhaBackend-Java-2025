package jorge.rinha.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jorge.rinha.dto.request.PaymentProcessorRequest;
import reactor.core.publisher.Mono;

@Service
public class PaymentProcessorService {
	
	
	@Value("${url.payment.processor.default}")
	private String urlProcessorDefault;
	
	@Value("${url.payment.processor.fallback}")
	private String urlProcessorFallback;
	
	private final WebClient webClient;
    private final RedisService redisService;

    
	public PaymentProcessorService(WebClient webClient, RedisService redisService) {
		this.webClient = webClient;
		this.redisService = redisService;
	}
	
	
	public Boolean processor(PaymentProcessorRequest paymentRequest, String jsonPayment) {
		
		for(int i = 0;i<13;i++) {
			if(callApiDefault(jsonPayment)) {
				redisService.saveDefault(paymentRequest);
				return true;
			}
		}
		
		if(callApiFallBack(jsonPayment)) {
			redisService.saveDefault(paymentRequest);
			return true;
		}
		
		
		return false;
	}

	public Boolean callApiFallBack(String json) {
        return webClient.post()
                .uri(urlProcessorFallback)
                .bodyValue(json)
                .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                .timeout(Duration.ofSeconds(8))
                .onErrorReturn(false)
                .block();
        
    }

    public Boolean callApiDefault(String json) {
        return webClient.post()
                .uri(urlProcessorDefault)
                .bodyValue(json)
                .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                .timeout(Duration.ofSeconds(8))
                .onErrorReturn(false)
                .block();
    };


    public String convertObjetoParaJson(PaymentProcessorRequest request) {
        return """
                {
                  "correlationId": "%s",
                  "amount": %s
                }
                """.formatted(
                escape(request.correlationId()),
                request.amount().toPlainString()
                
        ).replace("\n", "").replace("  ", "");
    }
    
    private String escape(String value) {
        return value.replace("\"", "\\\"");
    }
   
}
