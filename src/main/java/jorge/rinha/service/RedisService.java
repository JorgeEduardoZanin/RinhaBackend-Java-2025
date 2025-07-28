package jorge.rinha.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.dto.request.PaymentCompleted;
import jorge.rinha.dto.response.PaymentResponse;
import jorge.rinha.dto.response.PaymentSummaryResponse;

@Service
public class RedisService {

    private final RedisTemplate<String, Integer> redisTotalRequest;
    private final RedisTemplate<String, BigDecimal> redisTotalAmount;
    
	BlockingQueue<PaymentCompleted> queue = new ArrayBlockingQueue<>(2200);

    public RedisService(RedisTemplate<String, Integer> redisTotalRequest,
                        RedisTemplate<String, BigDecimal> redisTotalAmount) {
        this.redisTotalRequest = redisTotalRequest;
        this.redisTotalAmount = redisTotalAmount;
        
        for (int i = 0; i < 25; i++) {
			Thread.startVirtualThread(this::queueManager);
			
		}
    }

    public void queueManager() {
    	while (true) {
			var paymentCompleted = getOutOfQueue();
			if(paymentCompleted.type() == PaymentCompleted.PaymentType.DEFAULT) {
				saveDefault(paymentCompleted.amount());
			}else {
				saveFallback(paymentCompleted.amount());
			}
		}
    }
    
    
    public PaymentCompleted getOutOfQueue() {
    	try {
    		return queue.take();
		} catch (Exception e) {
			 throw new RuntimeException(e);
		}
    	
    }
    
    public void getInQueue(PaymentCompleted request) {
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
    
    public void choosePaymentType() {
    	
    }

    public void saveDefault(BigDecimal value) {

        redisTotalRequest.opsForValue().increment("totalRequestDefault", 1);

        BigDecimal atual = redisTotalAmount.opsForValue().get("totalAmountDefault");
        if (atual == null) {
            atual = BigDecimal.ZERO;
        }

        BigDecimal newTotal = atual.add(value);
        redisTotalAmount.opsForValue().set("totalAmountDefault", newTotal);
    }
    
    public void saveFallback(BigDecimal value) {

        redisTotalRequest.opsForValue().increment("totalRequestFallback", 1);

        BigDecimal atual = redisTotalAmount.opsForValue().get("totalAmountFallback");
        if (atual == null) {
            atual = BigDecimal.ZERO;
        }

        BigDecimal newTotal = atual.add(value);
        redisTotalAmount.opsForValue().set("totalAmountFallback", newTotal);
    }

    public PaymentSummaryResponse findSummary() {
        Integer requestsDefault = redisTotalRequest.opsForValue().get("totalRequestDefault");
        Integer requestsFallback = redisTotalRequest.opsForValue().get("totalRequestFallback");
        
        requestsDefault = (requestsDefault != null) ? requestsDefault : 0;
        requestsFallback = (requestsFallback != null) ? requestsFallback : 0;
        
        BigDecimal valueFallback = redisTotalAmount.opsForValue().get("totalAmountFallback");
        BigDecimal valueDefault = redisTotalAmount.opsForValue().get("totalAmountDefault");
        
        valueFallback = (valueFallback != null) ? valueFallback : BigDecimal.ZERO;
        valueDefault = (valueDefault != null) ? valueDefault : BigDecimal.ZERO;
        
        return new PaymentSummaryResponse(new PaymentResponse(valueDefault, requestsDefault), new PaymentResponse(valueFallback ,requestsFallback));
    }
}
