package jorge.rinha.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jorge.rinha.dto.request.FullPaymentProcessorRequest;
import jorge.rinha.dto.response.PaymentResponse;
import jorge.rinha.dto.response.PaymentSummaryResponse;
import jorge.rinha.dto.response.RedisRepositoryResponse;
import jorge.rinha.enums.PaymentType;

@Service
public class RedisRepository {
    private final RedisTemplate<String, String> redis;
    private static final String DEFAULT_KEY = "payments:default";
    private static final String FALLBACK_KEY = "payments:fallback";
    private final LinkedBlockingQueue<RedisRepositoryResponse> queue = new LinkedBlockingQueue<>();

    public RedisRepository(RedisTemplate<String, String> redis) {
        this.redis = redis;
        for (int i = 0; i < 3; i++) {
            Thread.startVirtualThread(this::queueManager);
        }
    }

    private void queueManager() {
        while (true) {
        	 RedisRepositoryResponse payment = getOutOfQueue();
            if (payment.paymentType() == PaymentType.DEFAULT) {
                saveDefault(payment.req());
            } else {
                saveFallback(payment.req());
            }
        }
    }

    private RedisRepositoryResponse getOutOfQueue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while taking from queue", e);
        }
    }

    public void getInQueue(RedisRepositoryResponse request) {
        try {
            boolean success = queue.offer(request, 1, TimeUnit.SECONDS);
            if (!success) {
            	success = queue.offer(request, 1, TimeUnit.SECONDS);           
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void saveDefault(FullPaymentProcessorRequest req) {
        String data = buildData(req.request().correlationId(), req.request().amount(), true);
        redis.opsForZSet().add(DEFAULT_KEY, data, req.now().toEpochMilli());
    }

    public void saveFallback(FullPaymentProcessorRequest req) {
        String data = buildData(req.request().correlationId(), req.request().amount(), false);
        redis.opsForZSet().add(FALLBACK_KEY, data, req.now().toEpochMilli());
    }

    public PaymentSummaryResponse findSummary(Instant from, Instant to) {
        Instant start = from != null ? from : Instant.now().minusSeconds(300);
        Instant end = to != null ? to : Instant.now();

        Set<String> defaultSet = redis.opsForZSet()
            .rangeByScore(DEFAULT_KEY, start.toEpochMilli(), end.toEpochMilli());
        Set<String> fallbackSet = redis.opsForZSet()
            .rangeByScore(FALLBACK_KEY, start.toEpochMilli(), end.toEpochMilli());

        long countDefault = 0;
        BigDecimal sumDefault = BigDecimal.ZERO;
        for (String entry : defaultSet) {
            String[] parts = entry.split(":");
            BigDecimal amt = new BigDecimal(parts[1]).movePointLeft(2);
            sumDefault = sumDefault.add(amt);
            countDefault++;
        }

        long countFallback = 0;
        BigDecimal sumFallback = BigDecimal.ZERO;
        for (String entry : fallbackSet) {
            String[] parts = entry.split(":");
            BigDecimal amt = new BigDecimal(parts[1]).movePointLeft(2);
            sumFallback = sumFallback.add(amt);
            countFallback++;
        }

        PaymentResponse def = new PaymentResponse(sumDefault, (int) countDefault);
        PaymentResponse fb  = new PaymentResponse(sumFallback, (int) countFallback);
        return new PaymentSummaryResponse(def, fb);
    }

    private String buildData(String uuid, BigDecimal amount, boolean isDefault) {
        return new StringBuilder()
            .append(uuid)
            .append(":")
            .append(amount.multiply(BigDecimal.valueOf(100)).longValueExact())
            .append(":")
            .append(isDefault)
            .toString();
    }
    
    public void clearAll() {
        redis.getConnectionFactory()
             .getConnection()
             .flushDb();
    }
}
