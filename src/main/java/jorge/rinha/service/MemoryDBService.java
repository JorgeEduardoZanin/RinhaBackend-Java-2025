// src/main/java/jorge/rinha/service/PaymentSummaryService.java
package jorge.rinha.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jorge.rinha.dto.response.MemoryDatabaseResponse;
import jorge.rinha.dto.response.PaymentResponse;
import jorge.rinha.dto.response.PaymentSummaryResponse;
import jorge.rinha.enums.PaymentType;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class MemoryDBService {

    private final Deque<MemoryDatabaseResponse> store = new ConcurrentLinkedDeque<>();
    private final WebClient otherInstance;

    public MemoryDBService(WebClient.Builder webClientBuilder,
                                 @Value("${url.instance}") String urlOtherInstance) {
        this.otherInstance = webClientBuilder
            .baseUrl(urlOtherInstance)
            .build();
    }

    public void save(MemoryDatabaseResponse response) {
        store.addLast(response);
    }

    public void cleanMemory() {
        store.clear();
    }

    public PaymentSummaryResponse findSummary(Instant from, Instant to) {
        // 1) snapshot para evitar race conditions
        List<MemoryDatabaseResponse> snapshot = new ArrayList<>(store);

        // 2) cálculo local
        PaymentSummaryResponse local = calculateLocal(snapshot, from, to);

        // 3) busca remota com timeout + fallback
        Mono<PaymentSummaryResponse> remoteMono = otherInstance.get()
            .uri(uri -> uri
                .queryParam("from", from == null ? null : from.toString())
                .queryParam("to",   to   == null ? null : to.toString())
                .build())
            .exchangeToMono(resp ->
                resp.statusCode().is2xxSuccessful()
                  ? resp.bodyToMono(PaymentSummaryResponse.class)
                  : Mono.just(emptySummary()))
            .timeout(Duration.ofMillis(100))
            .onErrorResume(e -> Mono.just(emptySummary()));

        // 4) combinar e bloquear em boundedElastic
        return remoteMono
            .map(remote -> merge(local, remote))
            .publishOn(Schedulers.boundedElastic())
            .block();  // aqui você obtém a resposta síncrona
    }

    private PaymentSummaryResponse calculateLocal(
            List<MemoryDatabaseResponse> snapshot,
            Instant from, Instant to) {

        Instant effFrom = from != null ? from : Instant.MIN;
        Instant effTo   = to   != null ? to   : Instant.now();

        BigDecimal sumDef = BigDecimal.ZERO, sumFb = BigDecimal.ZERO;
        int cntDef = 0, cntFb = 0;

        for (var rec : snapshot) {
            Instant ts = rec.req().now();
            if (ts.isBefore(effFrom) || ts.isAfter(effTo)) continue;

            BigDecimal amt = rec.req().request().amount();
            if (rec.paymentType() == PaymentType.DEFAULT) {
                sumDef = sumDef.add(amt);
                cntDef++;
            } else {
                sumFb = sumFb.add(amt);
                cntFb++;
            }
        }

        return new PaymentSummaryResponse(
            new PaymentResponse(sumDef, cntDef),
            new PaymentResponse(sumFb,  cntFb)
        );
    }

    private PaymentSummaryResponse merge(
            PaymentSummaryResponse a,
            PaymentSummaryResponse b) {

        BigDecimal defAmt = a.defaultResponse().totalAmount()
                              .add(b.defaultResponse().totalAmount());
        int defCnt = a.defaultResponse().totalRequests()
                     + b.defaultResponse().totalRequests();

        BigDecimal fbAmt = a.fallback().totalAmount()
                             .add(b.fallback().totalAmount());
        int fbCnt = a.fallback().totalRequests()
                    + b.fallback().totalRequests();

        return new PaymentSummaryResponse(
            new PaymentResponse(defAmt, defCnt),
            new PaymentResponse(fbAmt,  fbCnt)
        );
    }

    private PaymentSummaryResponse emptySummary() {
        return new PaymentSummaryResponse(
            new PaymentResponse(BigDecimal.ZERO, 0),
            new PaymentResponse(BigDecimal.ZERO, 0)
        );
    }
    
    public PaymentSummaryResponse calculateLocalOnly(Instant from, Instant to) {
        List<MemoryDatabaseResponse> snapshot = new ArrayList<>(store);
        return calculateLocal(snapshot, from, to);
    }


    public PaymentSummaryResponse findInternal(Instant from, Instant to) {
        PaymentSummaryResponse local = calculateLocalOnly(from, to);

        Mono<PaymentSummaryResponse> remoteMono = otherInstance.get()
            .uri("/internal?from={from}&to={to}", from, to)
            .exchangeToMono(resp ->
            resp.statusCode().is2xxSuccessful()
              ? resp.bodyToMono(PaymentSummaryResponse.class)
              : Mono.just(emptySummary()))
        .timeout(Duration.ofMillis(100))
        .onErrorResume(e -> Mono.just(emptySummary()));


    return remoteMono
        .map(remote -> merge(local, remote))
        .publishOn(Schedulers.boundedElastic())
        .block(); 
}

}
