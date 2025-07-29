package jorge.rinha.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.dto.request.PaymentCompleted;
import jorge.rinha.dto.request.PaymentCompleted.PaymentType;
import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.dto.response.HealthResponse;

@Service
public class PaymentProcessorService {

	private volatile PaymentType paymentType = PaymentType.DEFAULT;
	
	//BlockingQueue<FullPaymentProcessorRequest> queue = new ArrayBlockingQueue<>(2200);
	private final LinkedBlockingQueue<FullPaymentProcessorRequest> queue = new LinkedBlockingQueue<>();
	private final AtomicBoolean healthStarted = new AtomicBoolean(false);
	
	private final WebClient defaultClient;
	private final WebClient fallbackClient;
    private final WebClient webClientDefaultHealth;
    private final WebClient webClientFallbackHealth;
	
    private final RedisService redisService;

    public PaymentProcessorService(
            WebClient.Builder webClientBuilder,
            RedisService redisService,
            @Value("${url.payment.processor.default}") String urlDefault,
            @Value("${url.payment.processor.fallback}") String urlFallback,
            @Value("${url.health.fallback}") String urlFallbackHealth,
            @Value("${url.health.default}") String urlDefaultHealth) {

        this.defaultClient  = webClientBuilder.baseUrl(urlDefault).build();
        this.fallbackClient = webClientBuilder.baseUrl(urlFallback).build();
        this.redisService   = redisService;
        this.webClientDefaultHealth  = webClientBuilder.baseUrl(urlDefaultHealth).build();
        this.webClientFallbackHealth = webClientBuilder.baseUrl(urlFallbackHealth).build();
        
        for (int i = 0; i < 55; i++) {
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
    		if (healthStarted.compareAndSet(false, true)) {
                startHealthChecks();
            }
    		
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
		
		
		if(paymentType == PaymentType.DEFAULT) {
		for(int i = 0;i<3;i++) {
			if(apiDefault(jsonPayment)) {
				redisService.getInQueue(new PaymentCompleted(req.request().amount(), PaymentType.DEFAULT));
				return true;
			}
		}}else {
			if(apiFallBack(jsonPayment)) {
				redisService.getInQueue(new PaymentCompleted(req.request().amount(), PaymentType.FALLBACK));
				return true;
			}
		}
		
		return false;
	}

	public Boolean apiFallBack(String json) {
	    return fallbackClient.post()
	            .bodyValue(json)
	            .retrieve()
	            .toBodilessEntity()
	            .map(resp -> resp.getStatusCode().is2xxSuccessful())
	            .timeout(Duration.ofSeconds(12))
	            .onErrorReturn(false)
	            .block();
        
    }

    public Boolean apiDefault(String json) {
    	  return defaultClient.post()
    	            .bodyValue(json)
    	            .retrieve()
    	            .toBodilessEntity()
    	            .map(resp -> resp.getStatusCode().is2xxSuccessful())
    	            .timeout(Duration.ofSeconds(12))
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
    
 
    private void startHealthChecks() {
        Thread.startVirtualThread(() -> {

            for (int iteration = 0; iteration < 12; iteration++) {
                try {
                    HealthResponse defaultHealth = webClientDefaultHealth.get()
                        .retrieve()
                        .bodyToMono(HealthResponse.class)
                        .block();

                    HealthResponse fallbackHealth = webClientFallbackHealth.get()
                        .retrieve()
                        .bodyToMono(HealthResponse.class)
                        .block();

              
                    paymentType = (fallbackHealth.minResponseTime() < defaultHealth.minResponseTime())
                        ? PaymentType.FALLBACK
                        : PaymentType.DEFAULT;

                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    System.err.println("Erro no health‑check: " + ex.getMessage());
                }
            }
            System.out.println("Health‑checks concluídos.");
        });
    }
   
}
