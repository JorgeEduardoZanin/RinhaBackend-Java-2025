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
import jorge.rinha.dto.request.PaymentCompleted;
import jorge.rinha.dto.request.PaymentCompleted.PaymentType;
import jorge.rinha.dto.request.PaymentProcessorRequest;

@Service
public class PaymentProcessorService {

	
	BlockingQueue<FullPaymentProcessorRequest> queue = new ArrayBlockingQueue<>(2000);
	
	private final WebClient defaultClient;
	private final WebClient fallbackClient;
	
    private final RedisService redisService;

    public PaymentProcessorService(
            WebClient.Builder webClientBuilder,
            RedisService redisService,
            @Value("${url.payment.processor.default}") String urlDefault,
            @Value("${url.payment.processor.fallback}") String urlFallback) {

        this.defaultClient  = webClientBuilder.baseUrl(urlDefault).build();
        this.fallbackClient = webClientBuilder.baseUrl(urlFallback).build();
        this.redisService   = redisService;
        
        for (int i = 0; i < 20; i++) {
			Thread.startVirtualThread(this::queueManager);
		}
    }
    
    public void queueManager() {
    	while (true) {
			var paymentReq = getOutOfQueue();
			processor(paymentReq, paymentReq.json());
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
			boolean success =  queue.offer(request, 1, TimeUnit.SECONDS);
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
    
	public Boolean processor(FullPaymentProcessorRequest req, String jsonPayment) {
		
		for(int i = 0;i<5;i++) {
			if(apiDefault(jsonPayment)) {
				redisService.getInQueue(new PaymentCompleted(req.request().amount(), PaymentType.DEFAULT));
				return true;
			}
		}
		
		if(apiFallBack(jsonPayment)) {
			redisService.getInQueue(new PaymentCompleted(req.request().amount(), PaymentType.FALLBACK));
			return true;
		}
		

		return false;
	}

	public Boolean apiFallBack(String json) {
	    return fallbackClient.post()
	            .bodyValue(json)
	            .retrieve()
	            .toBodilessEntity()
	            .map(resp -> resp.getStatusCode().is2xxSuccessful())
	            .timeout(Duration.ofSeconds(10))
	            .onErrorReturn(false)
	            .block();
        
    }

    public Boolean apiDefault(String json) {
    	  return defaultClient.post()
    	            .bodyValue(json)
    	            .retrieve()
    	            .toBodilessEntity()
    	            .map(resp -> resp.getStatusCode().is2xxSuccessful())
    	            .timeout(Duration.ofSeconds(2))
    	            .onErrorReturn(false)
    	            .block();
    };


    public String ReqToJsonString(PaymentProcessorRequest request) {
        return """
                {
                "correlationId": "%s",
                "amount": %s,
                "requestedAt": "%s"
        		}
        		""".formatted(
                escapeForJson(request.correlationId()),
                request.amount().toPlainString(),
                Instant.now().toString()
                
        ).replace("\n", "").replace("  ", "");
    }
    
    public String escapeForJson (String value) {
        return value.replace("\"", "\\\"");
    }
   
}
