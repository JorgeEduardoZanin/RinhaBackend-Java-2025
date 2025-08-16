package jorge.rinha.service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.dto.response.MemoryDatabaseResponse;
import jorge.rinha.enums.PaymentType;


@Service
public class PaymentProcessorService {

	private final AtomicReference<PaymentType> paymentType = new AtomicReference<>(PaymentType.DEFAULT);

	 private final JsonFactory jf = new JsonFactory();
	private final BlockingQueue<FullPaymentProcessorRequest> queue = new ArrayBlockingQueue<>(10000);

	private final WebClient defaultClient;
	private final WebClient fallbackClient;
	

	
	private final MemoryDBService memoryDbService;
	
	private final Duration  durationDefault = Duration.ofSeconds(2);
	private final Duration  durationFallback = Duration.ofMillis(100);

	public PaymentProcessorService(WebClient.Builder webClientBuilder,
			@Value("${url.payment.processor.default}") String urlDefault,
			@Value("${url.payment.processor.fallback}") String urlFallback,
			MemoryDBService memoryDbService) {
		
			

		this.defaultClient = webClientBuilder.baseUrl(urlDefault).build();
		this.fallbackClient = webClientBuilder.baseUrl(urlFallback).build();
		this.memoryDbService = memoryDbService;
		
		//Thread.startVirtualThread(this::checkHealth);
		
		for (int i = 0; i < 15; i++) {
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
		
		queue.offer(request);
					
	}

	public void processor(FullPaymentProcessorRequest req) {

		//if (paymentType.get() == PaymentType.DEFAULT) {
		for(int i = 0; i<5;i++) {
				if (apiDefault(req.json())) {
					memoryDbService.save(new MemoryDatabaseResponse(req.now(), req.amount(), PaymentType.DEFAULT));
					return;
				}
		}
			//}
		
			if(apiFallBack(req.json())) {
					memoryDbService.save(new MemoryDatabaseResponse(req.now(), req.amount(), PaymentType.FALLBACK));
					return;
			}
			
			getInQueue(req);
		
	}

	public Boolean apiFallBack(byte[] json) {

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

	public Boolean apiDefault(byte[] json) {    
		
		Boolean success = defaultClient.post()
				.bodyValue(json)
				.retrieve()
				.toBodilessEntity()
				.map(resp -> resp.getStatusCode().is2xxSuccessful())
				.timeout(durationDefault)
				.onErrorReturn(false).block();
		     
  	    return success;
	};


	
	public FullPaymentProcessorRequest toFullRequest(PaymentProcessorRequest request) {
		Instant now = Instant.now();
	    try (ByteArrayOutputStream out = new ByteArrayOutputStream(128);
	         JsonGenerator gen = jf.createGenerator(out)) {

	        gen.writeStartObject();
	        gen.writeStringField("correlationId", request.correlationId());

	        gen.writeNumberField("amount", request.amount());
	        gen.writeStringField("requestedAt", now.toString());
	        gen.writeEndObject();
	        gen.flush();

	        byte[] jsonBytes = out.toByteArray();
	        return new FullPaymentProcessorRequest(jsonBytes, now,request.correlationId(), request.amount());
	    }catch (Exception e) {
			System.out.println(e);
		}
	    	return null;
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
