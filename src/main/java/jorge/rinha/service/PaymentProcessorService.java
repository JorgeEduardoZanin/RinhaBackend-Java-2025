package jorge.rinha.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.dto.response.MemoryDatabaseResponse;
import jorge.rinha.enums.PaymentType;


@Service
public class PaymentProcessorService {

	private final AtomicReference<PaymentType> paymentType = new AtomicReference<>(PaymentType.DEFAULT);


	private final BlockingQueue<FullPaymentProcessorRequest> queue = new ArrayBlockingQueue<>(10000);

	private final WebClient defaultClient;
	private final WebClient fallbackClient;
	

	
	private final MemoryDBService memoryDbService;
	
	private final Duration  durationDefault = Duration.ofSeconds(12);
	private final Duration  durationFallback = Duration.ofMillis(333);

	public PaymentProcessorService(WebClient.Builder webClientBuilder,
			@Value("${url.payment.processor.default}") String urlDefault,
			@Value("${url.payment.processor.fallback}") String urlFallback,
			MemoryDBService memoryDbService) {
		
			

		this.defaultClient = webClientBuilder.baseUrl(urlDefault).build();
		this.fallbackClient = webClientBuilder.baseUrl(urlFallback).build();
		this.memoryDbService = memoryDbService;
		
		Thread.startVirtualThread(this::checkHealth);
		
		for (int i = 0; i < 13; i++) {
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

			queue.offer(request, 1, TimeUnit.SECONDS);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void processor(FullPaymentProcessorRequest req) {

		if (paymentType.get() == PaymentType.DEFAULT) {
				if (apiDefault(req.json())) {
					memoryDbService.save(new MemoryDatabaseResponse(req, PaymentType.DEFAULT));
					return;
				}
			}
		
			if (apiFallBack(req.json())) {
					memoryDbService.save(new MemoryDatabaseResponse(req, PaymentType.FALLBACK));
					return;
			}
		
	}

	public Boolean apiFallBack(String json) {

	    Boolean success = fallbackClient
	        .post()
	        .bodyValue(json)
	        .retrieve()
	        .toBodilessEntity()
	        .map(resp -> resp.getStatusCode().is2xxSuccessful())
	        .timeout(durationFallback)
	        .onErrorReturn(false)
	        .block();                                                    

	    return success;
	}

	public Boolean apiDefault(String json) {    
		
		Boolean success = defaultClient.post()
				.bodyValue(json)
				.retrieve()
				.toBodilessEntity()
				.map(resp -> resp.getStatusCode().is2xxSuccessful())
				.timeout(durationDefault)
				.onErrorReturn(false).block();
		     
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

	public void checkHealth() {
		
		while(true) {
		
	    String json = """
	        {
	          "correlationId": "invalid",
	          "amount": invalid,
	          "requestedAt": "3000-01-01T00:00:00Z"
	        }
	        """
	        .formatted()
	        .replace("\n", "")
	        .replace("  ", "");
    	
		long startFallback = System.nanoTime(); 
		
		var fallbackOk = fallbackClient
			        .post()
			        .bodyValue(json)
			        .retrieve()
			        .toBodilessEntity()
			        .map(resp -> resp.getStatusCode().is2xxSuccessful())
			        .timeout(durationFallback)
		            .onErrorReturn(false)    
			        .block(); 
		 
		 long elapsedMsFallback = (System.nanoTime() - startFallback) / 1_000_000;  
		 
		 long startDefault = System.nanoTime(); 
		 
		 var defaultOk = defaultClient.post()
					.bodyValue(json)
					.retrieve()
					.toBodilessEntity()
					.map(resp -> resp.getStatusCode().is2xxSuccessful())
					.timeout(durationDefault)
		            .onErrorReturn(false)    
					.block();
		
		long elapsedMsDefault = (System.nanoTime() - startDefault) / 1_000_000;  
		
		if (elapsedMsDefault > 100 && elapsedMsDefault < 400L) {
			paymentType.set(PaymentType.FALLBACK);
		}else {
			paymentType.set(PaymentType.DEFAULT);
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
			
		} 
		
    }
	
}
