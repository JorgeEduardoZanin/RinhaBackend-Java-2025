package jorge.rinha.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

	private final BlockingQueue<FullPaymentProcessorRequest> queue = new ArrayBlockingQueue<>(5000);

	private final WebClient defaultClient;
	private final WebClient fallbackClient;
	
	private final RedisRepository redis;
	
	private final Duration  durationDefault = Duration.ofSeconds(12);
	private final Duration  durationFallback = Duration.ofMillis(200);

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
			processor(paymentReq);
			
			
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
				queue.offer(request, 1, TimeUnit.SECONDS);
				}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void processor(FullPaymentProcessorRequest req) {

		if (paymentType == PaymentType.DEFAULT) {
				if (apiDefault(req.json())) {
					redis.getInQueue(new RedisRepositoryResponse(req, PaymentType.DEFAULT));
					return;
				}
			}
		
			if (apiFallBack(req.json())) {
				redis.getInQueue(new RedisRepositoryResponse(req, PaymentType.FALLBACK));
				return;
			}
		
	}

	public Boolean apiFallBack(String json) {

	    long start = System.nanoTime();                                   

	    Boolean success = fallbackClient
	        .post()
	        .bodyValue(json)
	        .retrieve()
	        .toBodilessEntity()
	        .map(resp -> resp.getStatusCode().is2xxSuccessful())
	        .timeout(durationFallback)
	        .onErrorReturn(false)
	        .block();                                                    

	    long elapsedMs = (System.nanoTime() - start) / 1_000_000;         

	    if(elapsedMs > 100L){
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
				.timeout(durationDefault)
				.onErrorReturn(false).block();
		
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;       

	    if(elapsedMs < 100L) {
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

}
