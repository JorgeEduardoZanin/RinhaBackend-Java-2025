package jorge.rinha.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import jorge.rinha.dto.response.PaymentRecord;
import jorge.rinha.dto.response.PaymentResponse;
import jorge.rinha.dto.response.PaymentSummaryResponse;

@Service
public class MemoryDBService {

	   // Armazena todos os registros em memória
    private final Deque<PaymentRecord> store = new ConcurrentLinkedDeque<>();

    // Contadores globais (podem ser usados em outras métricas, mas aqui não são essenciais)
    private  AtomicLong totalRequestsDefault  = new AtomicLong(0);
    private  AtomicLong totalRequestsFallback = new AtomicLong(0);
    private  AtomicReference<BigDecimal> totalAmountDefault  = new AtomicReference<>(BigDecimal.ZERO);
    private  AtomicReference<BigDecimal> totalAmountFallback = new AtomicReference<>(BigDecimal.ZERO);

   
    public void cleanMemory() {
        totalRequestsDefault.set(0);
        totalRequestsFallback.set(0);
        totalAmountDefault.set(BigDecimal.ZERO);
        totalAmountFallback.set(BigDecimal.ZERO);
    }
    
    
    public void saveDefault(BigDecimal amount) {
        Instant now = Instant.now();
        store.addLast(new PaymentRecord(now, amount, false));

        totalRequestsDefault.incrementAndGet();
        totalAmountDefault.getAndUpdate(prev -> prev.add(amount));

      
    }

    public void saveFallback(BigDecimal amount) {
        Instant now = Instant.now();
        store.addLast(new PaymentRecord(now, amount, true));

        totalRequestsFallback.incrementAndGet();
        totalAmountFallback.getAndUpdate(prev -> prev.add(amount));

    }

 
    public PaymentSummaryResponse getSummary(Instant from, Instant to) {
        Instant effectiveFrom = (from != null) ? from : Instant.MIN;
        Instant effectiveTo   = (to   != null) ? to   : Instant.now();

        BigDecimal sumDefault  = BigDecimal.ZERO;
        BigDecimal sumFallback = BigDecimal.ZERO;
        int countDefault       = 0;
        int countFallback      = 0;

        for (PaymentRecord rec : store) {
            Instant ts = rec.timestamp;
            if (ts.isBefore(effectiveFrom) || ts.isAfter(effectiveTo)) {
                continue;
            }
            if (rec.fallback) {
                sumFallback = sumFallback.add(rec.amount);
                countFallback++;
            } else {
                sumDefault = sumDefault.add(rec.amount);
                countDefault++;
            }
        }

        PaymentResponse def = new PaymentResponse(sumDefault, countDefault);
        PaymentResponse fb  = new PaymentResponse(sumFallback, countFallback);
        return new PaymentSummaryResponse(def, fb);
    }
}

    
    
    

