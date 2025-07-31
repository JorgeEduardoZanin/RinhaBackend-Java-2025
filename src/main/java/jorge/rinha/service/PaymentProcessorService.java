package jorge.rinha.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.repository.RedisRepository;
import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.dto.response.RedisRepositoryResponse;
import jorge.rinha.enums.PaymentType;


@Service
public class PaymentProcessorService {

	private volatile PaymentType paymentType = PaymentType.DEFAULT;


	private final LinkedBlockingQueue<FullPaymentProcessorRequest> queue = new LinkedBlockingQueue<>();


	private final WebClient defaultClient;
	private final WebClient fallbackClient;
	
	private final RedisRepository redis;

	public PaymentProcessorService(WebClient.Builder webClientBuilder,
			@Value("${url.payment.processor.default}") String urlDefault,
			@Value("${url.payment.processor.fallback}") String urlFallback,
			RedisRepository redis) {
		
			

		this.defaultClient = webClientBuilder.baseUrl(urlDefault).build();
		this.fallbackClient = webClientBuilder.baseUrl(urlFallback).build();
		this.redis=redis;
	

		for (int i = 0; i < 25; i++) {
			Thread.startVirtualThread(this::queueManager);
		}
	}

	public void queueManager() {
		while (true) {
			var paymentReq = getOutOfQueue();
			if(!processor(paymentReq)){
				getInQueue(paymentReq);
			}
		}
	}

	public FullPaymentProcessorRequest getOutOfQueue() {
		try {
			return queue.take();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void getInQueue(FullPaymentProcessorRequest request) {
		try {

			boolean success = queue.offer(request, 1, TimeUnit.SECONDS);
			if (!success) {
				Thread.sleep(Duration.ofSeconds(2));
				queue.offer(request, 1, TimeUnit.SECONDS);
				if (!success) {
					System.out.println("Fila cheia! Pagamento descartado ou redirecionado.");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Boolean processor(FullPaymentProcessorRequest req) {

		if (paymentType == PaymentType.DEFAULT) {
			for (int i = 0; i < 3; i++) {
				if (apiDefault(req.json())) {
					//memoryDBService.saveDefault(req.request().amount());
					// redisService.getInQueue(new PaymentCompleted(req.request().amount(),
					// PaymentType.DEFAULT));
					redis.getInQueue(new RedisRepositoryResponse(req, PaymentType.DEFAULT));
					return true;
				}
			}
		} else {
			if (apiFallBack(req.json())) {
				
				redis.getInQueue(new RedisRepositoryResponse(req, PaymentType.FALLBACK));
				//memoryDBService.saveFallback(req.request().amount());
				// redisService.getInQueue(new PaymentCompleted(req.request().amount(),
				// PaymentType.FALLBACK));
				return true;
			}
		}
	
		return false;
	}

	public Boolean apiFallBack(String json) {

	    long start = System.nanoTime();                                   

	    Boolean success = fallbackClient
	        .post()
	        .bodyValue(json)
	        .retrieve()
	        .toBodilessEntity()
	        .map(resp -> resp.getStatusCode().is2xxSuccessful())
	        .timeout(Duration.ofSeconds(12))
	        .onErrorReturn(false)
	        .block();                                                    

	    long elapsedMs = (System.nanoTime() - start) / 1_000_000;         

	    if((elapsedMs > 20L && elapsedMs <80 )){
	    	paymentType = PaymentType.DEFAULT;
	    }
	    
	    return success;

	}

	public Boolean apiDefault(String json) {

		long start = System.nanoTime();    
		
		Boolean success = defaultClient.post()
				.bodyValue(json)
				.retrieve()
				.toBodilessEntity()
				.map(resp -> resp.getStatusCode().is2xxSuccessful())
				.timeout(Duration.ofSeconds(12))
				.onErrorReturn(false).block();
		
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;       
	    
	    if(elapsedMs > 100L) {
	    	paymentType = PaymentType.FALLBACK;
	    }else {
			paymentType = PaymentType.DEFAULT;
		}
	    return success;

	};

	public FullPaymentProcessorRequest ReqToJsonString(PaymentProcessorRequest request) {
		Instant now = Instant.now();
		String json = """
				      {
				      "correlationId": "%s",
				      "amount": %s,
				      "requestedAt": "%s"
				}
				""".formatted(escapeForJson(request.correlationId()), request.amount().toPlainString(),
				now.toString()

		).replace("\n", "").replace("  ", "");
		
		return new FullPaymentProcessorRequest(request, json, now);
	}

	public String escapeForJson(String value) {
		return value.replace("\"", "\\\"");
	}

	/*private void startHealthChecks() {
		Thread.startVirtualThread(() -> {

			for (int i = 0; i < 12; i++) {
				try {
					String last = redis.opsForValue().get("paymenttype");
					if (last != null) {
						paymentType = PaymentType.valueOf(last.toUpperCase());
					}
					System.out.println("linha 174 - last from Redis: " + last);
					System.out.println("linha 175 - current paymentType: " + paymentType);

					// DEFAULT health
					Mono<HealthResponse> defaultMono = webClientDefaultHealth.get().retrieve()
							.bodyToMono(HealthResponse.class).timeout(Duration.ofMillis(111));

					Mono<HealthResponse> fallbackMono = webClientFallbackHealth.get().retrieve()
							.bodyToMono(HealthResponse.class).timeout(Duration.ofMillis((111)));

					Tuple2<HealthResponse, HealthResponse> both = Mono.zip(defaultMono, fallbackMono)
							// opcional: timeout global para os dois
							.timeout(Duration.ofSeconds(1)).block();

					HealthResponse defaultHealth = both.getT1();
					HealthResponse fallbackHealth = both.getT2();

					System.out.println("linha 180 - defaultHealth: " + defaultHealth);
					if (defaultHealth != null) {
						System.out.println("linha 181 - default.minResponseTime(): " + defaultHealth.minResponseTime());
					}

					System.out.println("linha 186 - fallbackHealth: " + fallbackHealth);
					if (fallbackHealth != null) {
						System.out
								.println("linha 187 - fallback.minResponseTime(): " + fallbackHealth.minResponseTime());
					}

					// só recalcula se ambos não forem null
					if (defaultHealth != null && fallbackHealth != null) {
						paymentType = (fallbackHealth.minResponseTime() < defaultHealth.minResponseTime())
								? PaymentType.FALLBACK
								: PaymentType.DEFAULT;
					} else {
						System.out.println("linha 192 - mantendo paymentType: " + paymentType);
					}

					redis.opsForValue().set("paymenttype", paymentType.toString());
					System.out.println("linha 194 - gravou paymentType: " + paymentType);

					Thread.sleep(5200);
				} catch (InterruptedException e) {
					System.out.println("linha 200 - healthChecks interrompido: " + e.getMessage());
					String paymentTypeRedis = redis.opsForValue().get("paymenttype");
					System.out.println("linha 201 - paymentType após interrupção: " + paymentTypeRedis);
					if (paymentTypeRedis != null) {
						paymentType = PaymentType.valueOf(paymentTypeRedis.toUpperCase());
					}
					Thread.currentThread().interrupt();
					break;
				} catch (Exception ex) {
					System.out.println("linha 207 - Erro geral durante healthCheck: " + ex.getMessage());
					String paymentTypeRedis = redis.opsForValue().get("paymenttype");
					System.out.println("linha 208 - paymentType após erro geral: " + paymentTypeRedis);
					if (paymentTypeRedis != null) {
						paymentType = PaymentType.valueOf(paymentTypeRedis.toUpperCase());
					}
				}
			}

			System.out.println("Health-checks concluídos.");
			paymentType = PaymentType.DEFAULT;
			healthStarted.set(false);
		});
	}

	public HealthResponse teste() {
		var fallbackHealth = webClientFallbackHealth.get().retrieve().bodyToMono(HealthResponse.class)
				.timeout(Duration.ofSeconds(10)).block();
		return fallbackHealth;
	}*/

}
