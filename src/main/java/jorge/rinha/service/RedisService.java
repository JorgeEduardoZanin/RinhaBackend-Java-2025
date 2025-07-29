package jorge.rinha.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    private final RedisTemplate<String, Long> redisTotalAmountCents;


    private static final BigDecimal CENT_FACTOR = BigDecimal.valueOf(100);
    private final LinkedBlockingQueue<PaymentCompleted> queue = new LinkedBlockingQueue<>();

    //private final BlockingQueue<PaymentCompleted> queue = new ArrayBlockingQueue<>(2200);

    public RedisService(RedisTemplate<String, Integer> redisTotalRequest,
                        RedisTemplate<String, Long> redisTotalAmountCents) {
        this.redisTotalRequest = redisTotalRequest;
        this.redisTotalAmountCents = redisTotalAmountCents;

        for (int i = 0; i < 25; i++) {
            Thread.startVirtualThread(this::queueManager);
        }
    }

    private void queueManager() {
        while (true) {
            PaymentCompleted payment = getOutOfQueue();
            if (payment.type() == PaymentCompleted.PaymentType.DEFAULT) {
                saveDefault(payment.amount());
            } else {
                saveFallback(payment.amount());
            }
        }
    }

    private PaymentCompleted getOutOfQueue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while taking from queue", e);
        }
    }

    public void getInQueue(PaymentCompleted request) {
        try {
            boolean success = queue.offer(request, 1, TimeUnit.SECONDS);
            if (!success) {
                Thread.sleep(Duration.ofSeconds(2).toMillis());
                success = queue.offer(request, 1, TimeUnit.SECONDS);
                if (!success) {
                    System.out.println("Fila cheia! Pagamento descartado ou redirecionado.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void saveDefault(BigDecimal value) {

        redisTotalRequest.opsForValue().increment("totalRequestDefault", 1);


        long cents = value.multiply(CENT_FACTOR).longValueExact();
        redisTotalAmountCents.opsForValue()
            .increment("totalAmountDefaultInCents", cents);
    }

    public void saveFallback(BigDecimal value) {
        redisTotalRequest.opsForValue().increment("totalRequestFallback", 1);
        long cents = value.multiply(CENT_FACTOR).longValueExact();
        redisTotalAmountCents.opsForValue()
            .increment("totalAmountFallbackInCents", cents);
    }

    public PaymentSummaryResponse findSummary() {
    
        Integer requestsDefault = redisTotalRequest.opsForValue().get("totalRequestDefault");
        Integer requestsFallback = redisTotalRequest.opsForValue().get("totalRequestFallback");
        requestsDefault = (requestsDefault != null) ? requestsDefault : 0;
        requestsFallback = (requestsFallback != null) ? requestsFallback : 0;

   
        Long centsDefault = redisTotalAmountCents.opsForValue().get("totalAmountDefaultInCents");
        Long centsFallback = redisTotalAmountCents.opsForValue().get("totalAmountFallbackInCents");
        centsDefault = (centsDefault != null) ? centsDefault : 0L;
        centsFallback = (centsFallback != null) ? centsFallback : 0L;


        BigDecimal valueDefault = BigDecimal.valueOf(centsDefault)
            .divide(CENT_FACTOR, 2, RoundingMode.UNNECESSARY);
        BigDecimal valueFallback = BigDecimal.valueOf(centsFallback)
            .divide(CENT_FACTOR, 2, RoundingMode.UNNECESSARY);

        return new PaymentSummaryResponse(
            new PaymentResponse(valueDefault, requestsDefault),
            new PaymentResponse(valueFallback, requestsFallback)
        );
    }
}
