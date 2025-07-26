package jorge.rinha.service;

import java.math.BigDecimal;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jorge.rinha.dto.request.PaymentProcessorRequest;
import jorge.rinha.dto.response.PaymentResponse;
import jorge.rinha.dto.response.PaymentSummaryResponse;

@Service
public class RedisService {

    private final RedisTemplate<String, Integer> redisTotalRequest;
    private final RedisTemplate<String, BigDecimal> redisTotalAmount;

    public RedisService(RedisTemplate<String, Integer> redisTotalRequest,
                        RedisTemplate<String, BigDecimal> redisTotalAmount) {
        this.redisTotalRequest = redisTotalRequest;
        this.redisTotalAmount = redisTotalAmount;
    }

    public void saveDefault(PaymentProcessorRequest req) {

        redisTotalRequest.opsForValue().increment("totalRequestDefault", 1);

        BigDecimal atual = redisTotalAmount.opsForValue().get("totalAmountDefault");
        if (atual == null) {
            atual = BigDecimal.ZERO;
        }

        BigDecimal novoTotal = atual.add(req.amount());
        redisTotalAmount.opsForValue().set("totalAmountDefault", novoTotal);
    }
    
    public void saveFallback(PaymentProcessorRequest req) {

        redisTotalRequest.opsForValue().increment("totalRequestFallback", 1);

        BigDecimal atual = redisTotalAmount.opsForValue().get("totalAmountFallback");
        if (atual == null) {
            atual = BigDecimal.ZERO;
        }

        BigDecimal novoTotal = atual.add(req.amount());
        redisTotalAmount.opsForValue().set("totalAmountFallback", novoTotal);
    }

    public PaymentSummaryResponse findSummary() {
        Integer requestsDefault = redisTotalRequest.opsForValue().get("totalRequestDefault");
        Integer requestsFallback = redisTotalRequest.opsForValue().get("totalRequestFallback");
        
        requestsDefault = (requestsDefault != null) ? requestsDefault : 0;
        requestsFallback = (requestsFallback != null) ? requestsFallback : 0;
        
        BigDecimal valueFallback = redisTotalAmount.opsForValue().get("totalAmountFallback");
        BigDecimal valueDefault = redisTotalAmount.opsForValue().get("totalAmountDefault");
        
        return new PaymentSummaryResponse(new PaymentResponse(requestsDefault, valueDefault), new PaymentResponse(requestsFallback, valueFallback));
    }
}
