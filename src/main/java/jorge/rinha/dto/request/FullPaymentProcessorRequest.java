package jorge.rinha.dto.request;

import java.time.Instant;

public record FullPaymentProcessorRequest(PaymentProcessorRequest request, String json, Instant now) {

}
